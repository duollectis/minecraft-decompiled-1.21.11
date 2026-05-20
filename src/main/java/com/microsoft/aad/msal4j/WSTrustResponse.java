package com.microsoft.aad.msal4j;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class WSTrustResponse {
   private static final Logger log = LoggerFactory.getLogger(WSTrustResponse.class);
   public static final String SAML1_ASSERTION = "urn:oasis:names:tc:SAML:1.0:assertion";
   private String faultMessage;
   private boolean errorFound;
   private String errorCode;
   private String token;
   private String tokenType;

   private WSTrustResponse() {
   }

   String getFaultMessage() {
      return this.faultMessage;
   }

   boolean isErrorFound() {
      return this.errorFound;
   }

   String getErrorCode() {
      return this.errorCode;
   }

   String getToken() {
      return this.token;
   }

   String getTokenType() {
      return this.tokenType;
   }

   boolean isTokenSaml2() {
      return this.tokenType != null && !"urn:oasis:names:tc:SAML:1.0:assertion".equalsIgnoreCase(this.tokenType);
   }

   static WSTrustResponse parse(String response, WSTrustVersion version) throws Exception {
      WSTrustResponse responseValue = new WSTrustResponse();
      DocumentBuilderFactory builderFactory = SafeDocumentBuilderFactory.createInstance();
      builderFactory.setNamespaceAware(true);
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document xmlDocument = builder.parse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
      XPath xPath = XPathFactory.newInstance().newXPath();
      NamespaceContextImpl namespace = new NamespaceContextImpl();
      xPath.setNamespaceContext(namespace);
      if (parseError(responseValue, xmlDocument, xPath)) {
         if (StringHelper.isBlank(responseValue.errorCode)) {
            responseValue.errorCode = "NONE";
         }

         if (StringHelper.isBlank(responseValue.faultMessage)) {
            responseValue.faultMessage = "NONE";
         }

         throw new MsalServiceException(
            String.format("Server returned error in RSTR - ErrorCode: %s. FaultMessage: %s", responseValue.errorCode, responseValue.faultMessage.trim()),
            "wstrust_service_error"
         );
      } else {
         parseToken(responseValue, xmlDocument, xPath, version);
         return responseValue;
      }
   }

   private static void parseToken(WSTrustResponse responseValue, Document xmlDocument, XPath xPath, WSTrustVersion version) throws Exception {
      NodeList tokenTypeNodes = (NodeList)xPath.compile(version.responseTokenTypePath()).evaluate(xmlDocument, XPathConstants.NODESET);
      if (tokenTypeNodes.getLength() == 0) {
         String msg = "No TokenType elements found in RSTR";
         log.warn(msg);
      }

      for (int i = 0; i < tokenTypeNodes.getLength(); i++) {
         if (!StringHelper.isBlank(responseValue.token)) {
            String msg = "Found more than one returned token.  Using the first.";
            log.warn(msg);
            break;
         }

         Node tokenTypeNode = tokenTypeNodes.item(i);
         responseValue.tokenType = tokenTypeNode.getTextContent();
         if (StringHelper.isBlank(responseValue.tokenType)) {
            String msg = "Could not find token type in RSTR token";
            log.warn(msg);
         }

         NodeList requestedTokenNodes = (NodeList)xPath.compile(version.responseSecurityTokenPath())
            .evaluate(tokenTypeNode.getParentNode(), XPathConstants.NODESET);
         if (requestedTokenNodes.getLength() > 1) {
            throw new MsalClientException(
               String.format("Error parsing WSTrustResponse: Found too many RequestedSecurityToken nodes for token type %s", responseValue.tokenType),
               "wstrust_invalid_response"
            );
         }

         if (requestedTokenNodes.getLength() == 0) {
            String msg = "Unable to find RequestsSecurityToken element associated with TokenType element: " + responseValue.tokenType;
            log.warn(msg);
         } else {
            responseValue.token = innerXml(requestedTokenNodes.item(0));
            if (StringHelper.isBlank(responseValue.token)) {
               String msg = "Unable to find token associated with TokenType element: " + responseValue.tokenType;
               log.warn(msg);
            } else {
               String msg = "Found token of type: " + responseValue.tokenType;
               log.info(msg);
            }
         }
      }

      if (StringHelper.isBlank(responseValue.token)) {
         throw new MsalClientException("Error parsing WSTrustResponse: Unable to find any tokens in RSTR", "wstrust_invalid_response");
      }
   }

   private static boolean parseError(WSTrustResponse responseValue, Document xmlDocument, XPath xPath) throws XPathExpressionException {
      boolean errorFound = false;
      NodeList faultNodes = (NodeList)xPath.compile("//s:Envelope/s:Body/s:Fault/s:Reason").evaluate(xmlDocument, XPathConstants.NODESET);
      if (faultNodes.getLength() > 0) {
         responseValue.faultMessage = faultNodes.item(0).getTextContent();
         if (!StringHelper.isBlank(responseValue.faultMessage)) {
            responseValue.errorFound = true;
         }
      }

      NodeList subcodeNodes = (NodeList)xPath.compile("//s:Envelope/s:Body/s:Fault/s:Code/s:Subcode/s:Value").evaluate(xmlDocument, XPathConstants.NODESET);
      if (subcodeNodes.getLength() > 1) {
         throw new MsalClientException(
            String.format("Error parsing WSTrustResponse: Found too many fault code values: %s", subcodeNodes.getLength()), "wstrust_invalid_response"
         );
      } else {
         if (subcodeNodes.getLength() == 1) {
            responseValue.errorCode = subcodeNodes.item(0).getChildNodes().item(0).getTextContent();
            responseValue.errorCode = responseValue.errorCode.split(":")[1];
            errorFound = true;
         }

         return errorFound;
      }
   }

   static String innerXml(Node node) {
      StringBuilder resultBuilder = new StringBuilder();
      NodeList children = node.getChildNodes();

      try {
         Transformer transformer = TransformerFactory.newInstance().newTransformer();
         transformer.setOutputProperty("omit-xml-declaration", "yes");
         StringWriter sw = new StringWriter();
         StreamResult streamResult = new StreamResult(sw);

         for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            DOMSource source = new DOMSource(child);
            transformer.transform(source, streamResult);
            resultBuilder.append(sw.toString());
         }
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      return resultBuilder.toString().trim();
   }
}
