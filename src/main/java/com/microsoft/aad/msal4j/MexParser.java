package com.microsoft.aad.msal4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MexParser {
   private static final Logger log = LoggerFactory.getLogger(MexParser.class);
   private static final String TRANSPORT_BINDING_XPATH = "wsp:ExactlyOne/wsp:All/sp:TransportBinding";
   private static final String TRANSPORT_BINDING_2005_XPATH = "wsp:ExactlyOne/wsp:All/sp2005:TransportBinding";
   private static final String PORT_XPATH = "//wsdl:definitions/wsdl:service/wsdl:port";
   private static final String ADDRESS_XPATH = "wsa10:EndpointReference/wsa10:Address";
   private static final String SOAP_ACTION_XPATH = "wsdl:operation/soap12:operation/@soapAction";
   private static final String RST_SOAP_ACTION = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue";
   private static final String RST_SOAP_ACTION_2005 = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue";
   private static final String SOAP_TRANSPORT_XPATH = "soap12:binding/@transport";
   private static final String SOAP_HTTP_TRANSPORT_VALUE = "http://schemas.xmlsoap.org/soap/http";

   static BindingPolicy getPolicy(String mexResponse, MexParser.PolicySelector policySelector, boolean logPii) throws Exception {
      DocumentBuilderFactory builderFactory = SafeDocumentBuilderFactory.createInstance();
      builderFactory.setNamespaceAware(true);
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document xmlDocument = builder.parse(new ByteArrayInputStream(mexResponse.getBytes(StandardCharsets.UTF_8)));
      XPath xPath = XPathFactory.newInstance().newXPath();
      NamespaceContextImpl nameSpace = new NamespaceContextImpl();
      xPath.setNamespaceContext(nameSpace);
      Map<String, BindingPolicy> policies = policySelector.selectPolicies(xmlDocument, xPath, logPii);
      if (policies.isEmpty()) {
         log.debug("No matching policies");
         return null;
      } else {
         Map<String, BindingPolicy> bindings = getMatchingBindings(xmlDocument, xPath, policies, logPii);
         if (bindings.isEmpty()) {
            log.debug("No matching bindings");
            return null;
         } else {
            getPortsForPolicyBindings(xmlDocument, xPath, bindings, policies, logPii);
            return selectSingleMatchingPolicy(policies);
         }
      }
   }

   static BindingPolicy getPolicyFromMexResponseForIntegrated(String mexResponse, boolean logPii) throws Exception {
      return getPolicy(mexResponse, new MexParser.NegotiateAuthenticationPolicySelector(), logPii);
   }

   static BindingPolicy getWsTrustEndpointFromMexResponse(String mexResponse, boolean logPii) throws Exception {
      return getPolicy(mexResponse, new MexParser.WsTrustEndpointPolicySelector(), logPii);
   }

   private static BindingPolicy selectSingleMatchingPolicy(Map<String, BindingPolicy> policies) {
      BindingPolicy wstrust13 = null;
      BindingPolicy wstrust2005 = null;

      for (Entry<String, BindingPolicy> pair : policies.entrySet()) {
         if (pair.getValue().getUrl() != null) {
            if (pair.getValue().getVersion() == WSTrustVersion.WSTRUST13) {
               wstrust13 = pair.getValue();
            } else if (pair.getValue().getVersion() == WSTrustVersion.WSTRUST2005) {
               wstrust2005 = pair.getValue();
            }
         }
      }

      if (wstrust13 == null && wstrust2005 == null) {
         log.warn("No policies found with the url");
         return null;
      } else {
         return wstrust13 != null ? wstrust13 : wstrust2005;
      }
   }

   private static void getPortsForPolicyBindings(
      Document xmlDocument, XPath xPath, Map<String, BindingPolicy> bindings, Map<String, BindingPolicy> policies, boolean logPii
   ) throws Exception {
      NodeList portNodes = (NodeList)xPath.compile("//wsdl:definitions/wsdl:service/wsdl:port").evaluate(xmlDocument, XPathConstants.NODESET);
      if (portNodes.getLength() == 0) {
         log.warn("No ports found");
      } else {
         for (int i = 0; i < portNodes.getLength(); i++) {
            Node portNode = portNodes.item(i);
            String bindingId = portNode.getAttributes().getNamedItem("binding").getNodeValue();
            String[] bindingIdParts = bindingId.split(":");
            bindingId = bindingIdParts[bindingIdParts.length - 1];
            BindingPolicy trustPolicy = bindings.get(bindingId);
            if (trustPolicy != null) {
               BindingPolicy bindingPolicy = policies.get(trustPolicy.getUrl());
               if (bindingPolicy != null && StringHelper.isBlank(bindingPolicy.getUrl())) {
                  bindingPolicy.setVersion(trustPolicy.getVersion());
                  NodeList addressNodes = (NodeList)xPath.compile("wsa10:EndpointReference/wsa10:Address").evaluate(portNode, XPathConstants.NODESET);
                  if (addressNodes.getLength() <= 0) {
                     throw new MsalClientException("Error parsing WSTrustResponse: No address nodes on port", "wstrust_invalid_response");
                  }

                  String address = addressNodes.item(0).getTextContent();
                  if (address != null && address.toLowerCase().startsWith("https://")) {
                     bindingPolicy.setUrl(address.trim());
                  } else if (logPii) {
                     log.warn("Skipping insecure endpoint: " + address);
                  } else {
                     log.warn("Skipping insecure endpoint");
                  }
               }
            }
         }
      }
   }

   private static Map<String, BindingPolicy> getMatchingBindings(Document xmlDocument, XPath xPath, Map<String, BindingPolicy> policies, boolean logPii) throws XPathExpressionException {
      Map<String, BindingPolicy> bindings = new HashMap<>();
      NodeList nodeList = (NodeList)xPath.compile("//wsdl:definitions/wsdl:binding/wsp:PolicyReference").evaluate(xmlDocument, XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); i++) {
         Node node = nodeList.item(i);
         String uri = node.getAttributes().getNamedItem("URI").getNodeValue();
         if (policies.containsKey(uri)) {
            Node bindingNode = node.getParentNode();
            String bindingName = bindingNode.getAttributes().getNamedItem("name").getNodeValue();
            WSTrustVersion version = checkSoapActionAndTransport(xPath, bindingNode, logPii);
            if (version != WSTrustVersion.UNDEFINED) {
               BindingPolicy policy = new BindingPolicy("");
               policy.setUrl(uri);
               policy.setVersion(version);
               bindings.put(bindingName, policy);
            }
         }
      }

      return bindings;
   }

   private static WSTrustVersion checkSoapActionAndTransport(XPath xPath, Node bindingNode, boolean logPii) throws XPathExpressionException {
      NodeList soapTransportAttributes = null;
      String soapAction = null;
      String bindingName = bindingNode.getAttributes().getNamedItem("name").getNodeValue();
      NodeList soapActionAttributes = (NodeList)xPath.compile("wsdl:operation/soap12:operation/@soapAction").evaluate(bindingNode, XPathConstants.NODESET);
      if (soapActionAttributes.getLength() > 0) {
         soapAction = soapActionAttributes.item(0).getNodeValue();
         soapTransportAttributes = (NodeList)xPath.compile("soap12:binding/@transport").evaluate(bindingNode, XPathConstants.NODESET);
         if (soapTransportAttributes != null
            && soapTransportAttributes.getLength() > 0
            && soapTransportAttributes.item(0).getNodeValue().equalsIgnoreCase("http://schemas.xmlsoap.org/soap/http")) {
            if (soapAction.equalsIgnoreCase("http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")) {
               if (logPii) {
                  log.debug("Found binding matching Action and Transport: " + bindingName);
               } else {
                  log.debug("Found binding matching Action and Transport");
               }

               return WSTrustVersion.WSTRUST13;
            }

            if (soapAction.equalsIgnoreCase("http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue")) {
               if (logPii) {
                  log.debug("Binding node did not match soap Action or Transport: " + bindingName);
               } else {
                  log.debug("Binding node did not match soap Action or Transport");
               }

               return WSTrustVersion.WSTRUST2005;
            }
         }
      }

      return WSTrustVersion.UNDEFINED;
   }

   private static Map<String, BindingPolicy> selectUsernamePasswordPoliciesWithExpression(
      Document xmlDocument, XPath xPath, String xpathExpression, boolean logPii
   ) throws XPathExpressionException {
      Map<String, BindingPolicy> policies = new HashMap<>();
      NodeList nodeList = (NodeList)xPath.compile(xpathExpression).evaluate(xmlDocument, XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); i++) {
         String policy = checkPolicy(
            xPath, nodeList.item(i).getParentNode().getParentNode().getParentNode().getParentNode().getParentNode().getParentNode().getParentNode(), logPii
         );
         policies.put("#" + policy, new BindingPolicy("#" + policy));
      }

      return policies;
   }

   private static Map<String, BindingPolicy> selectIntegratedPoliciesWithExpression(Document xmlDocument, XPath xPath, String xpathExpression) throws XPathExpressionException {
      Map<String, BindingPolicy> policies = new HashMap<>();
      NodeList nodeList = (NodeList)xPath.compile(xpathExpression).evaluate(xmlDocument, XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); i++) {
         String policy = checkPolicyIntegrated(xPath, nodeList.item(i).getParentNode().getParentNode().getParentNode());
         policies.put("#" + policy, new BindingPolicy("#" + policy));
      }

      return policies;
   }

   private static String checkPolicy(XPath xPath, Node node, boolean logPii) throws XPathExpressionException {
      String policyId = null;
      Node id = node.getAttributes().getNamedItem("wsu:Id");
      NodeList transportBindingNodes = (NodeList)xPath.compile("wsp:ExactlyOne/wsp:All/sp:TransportBinding").evaluate(node, XPathConstants.NODESET);
      if (transportBindingNodes.getLength() == 0) {
         transportBindingNodes = (NodeList)xPath.compile("wsp:ExactlyOne/wsp:All/sp2005:TransportBinding").evaluate(node, XPathConstants.NODESET);
      }

      if (transportBindingNodes.getLength() > 0 && id != null) {
         policyId = id.getNodeValue();
         if (logPii) {
            log.debug("found matching policy id: " + policyId);
         } else {
            log.debug("found matching policy");
         }
      } else {
         String nodeValue = "none";
         if (id != null) {
            nodeValue = id.getNodeValue();
         }

         if (logPii) {
            log.debug("potential policy did not match required transport binding: " + nodeValue);
         } else {
            log.debug("potential policy did not match required transport binding");
         }
      }

      return policyId;
   }

   private static String checkPolicyIntegrated(XPath xPath, Node node) throws XPathExpressionException {
      Node id = node.getAttributes().getNamedItem("wsu:Id");
      return id.getNodeValue();
   }

   private static class NegotiateAuthenticationPolicySelector implements MexParser.PolicySelector {
      private NegotiateAuthenticationPolicySelector() {
      }

      @Override
      public Map<String, BindingPolicy> selectPolicies(Document xmlDocument, XPath xPath, boolean logPii) throws XPathExpressionException {
         String xpathExpression = "//wsdl:definitions/wsp:Policy/wsp:ExactlyOne/wsp:All/http:NegotiateAuthentication";
         return MexParser.selectIntegratedPoliciesWithExpression(xmlDocument, xPath, xpathExpression);
      }
   }

   private interface PolicySelector {
      Map<String, BindingPolicy> selectPolicies(Document var1, XPath var2, boolean var3) throws XPathExpressionException;
   }

   private static class WsTrustEndpointPolicySelector implements MexParser.PolicySelector {
      private WsTrustEndpointPolicySelector() {
      }

      @Override
      public Map<String, BindingPolicy> selectPolicies(Document xmlDocument, XPath xPath, boolean logPii) throws XPathExpressionException {
         String xpathExpression = "//wsdl:definitions/wsp:Policy/wsp:ExactlyOne/wsp:All/sp:SignedEncryptedSupportingTokens/wsp:Policy/sp:UsernameToken/wsp:Policy/sp:WssUsernameToken10";
         Map<String, BindingPolicy> policies = MexParser.selectUsernamePasswordPoliciesWithExpression(xmlDocument, xPath, xpathExpression, logPii);
         ((NamespaceContextImpl)xPath.getNamespaceContext()).modifyNameSpace("sp", "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy");
         xpathExpression = "//wsdl:definitions/wsp:Policy/wsp:ExactlyOne/wsp:All/sp:SignedSupportingTokens/wsp:Policy/sp:UsernameToken/wsp:Policy/sp:WssUsernameToken10";
         policies.putAll(MexParser.selectUsernamePasswordPoliciesWithExpression(xmlDocument, xPath, xpathExpression, logPii));
         return policies;
      }
   }
}
