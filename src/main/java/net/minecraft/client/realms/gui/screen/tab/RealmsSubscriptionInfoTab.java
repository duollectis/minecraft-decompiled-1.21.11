package net.minecraft.client.realms.gui.screen.tab;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.Subscription;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.gui.screen.RealmsConfigureWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Urls;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsSubscriptionInfoTab}.
 */
public class RealmsSubscriptionInfoTab extends GridScreenTab implements RealmsUpdatableTab {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int CONTENT_WIDTH = 200;
	private static final int COLUMN_SPACING = 2;
	private static final int ROW_SPACING = 6;
	public static final Text SUBSCRIPTION_TITLE = Text.translatable("mco.configure.world.subscription.tab");
	private static final Text
			SUBSCRIPTION_START_LABEL_TEXT =
			Text.translatable("mco.configure.world.subscription.start");
	private static final Text TIME_LEFT_LABEL_TEXT = Text.translatable("mco.configure.world.subscription.timeleft");
	private static final Text
			DAYS_LEFT_LABEL_TEXT =
			Text.translatable("mco.configure.world.subscription.recurring.daysleft");
	private static final Text
			EXPIRED_TEXT =
			Text.translatable("mco.configure.world.subscription.expired").formatted(Formatting.GRAY);
	private static final Text
			EXPIRES_IN_LESS_THAN_A_DAY_TEXT =
			Text.translatable("mco.configure.world.subscription.less_than_a_day").formatted(Formatting.GRAY);
	private static final Text UNKNOWN_TEXT = Text.translatable("mco.configure.world.subscription.unknown");
	private static final Text
			RECURRING_INFO_TEXT =
			Text.translatable("mco.configure.world.subscription.recurring.info");
	private final RealmsConfigureWorldScreen screen;
	private final MinecraftClient client;
	private final ButtonWidget deleteWorldButton;
	private final NarratedMultilineTextWidget subscriptionInfoTextWidget;
	private final TextWidget startDateTextWidget;
	private final TextWidget timeLeftLabelTextWidget;
	private final TextWidget daysLeftTextWidget;
	private RealmsServer serverData;
	private Text daysLeft = UNKNOWN_TEXT;
	private Text startDate = UNKNOWN_TEXT;
	private Subscription.@Nullable SubscriptionType type;

	public RealmsSubscriptionInfoTab(RealmsConfigureWorldScreen screen, MinecraftClient client, RealmsServer server) {
		super(SUBSCRIPTION_TITLE);
		this.screen = screen;
		this.client = client;
		this.serverData = server;
		GridWidget.Adder adder = this.grid.setRowSpacing(6).createAdder(1);
		TextRenderer textRenderer = screen.getTextRenderer();
		adder.add(new TextWidget(200, 9, SUBSCRIPTION_START_LABEL_TEXT, textRenderer));
		this.startDateTextWidget = adder.add(new TextWidget(200, 9, this.startDate, textRenderer));
		adder.add(EmptyWidget.ofHeight(2));
		this.timeLeftLabelTextWidget = adder.add(new TextWidget(200, 9, TIME_LEFT_LABEL_TEXT, textRenderer));
		this.daysLeftTextWidget = adder.add(new TextWidget(200, 9, this.daysLeft, textRenderer));
		adder.add(EmptyWidget.ofHeight(2));
		adder.add(
				ButtonWidget.builder(
						            Text.translatable("mco.configure.world.subscription.extend"),
						            button -> ConfirmLinkScreen.open(
								            screen,
								            Urls.getExtendJavaRealmsUrl(
										            server.remoteSubscriptionId,
										            client.getSession().getUuidOrNull()
								            )
						            )
				            )
				            .dimensions(0, 0, 200, 20)
				            .build()
		);
		adder.add(EmptyWidget.ofHeight(2));
		this.deleteWorldButton = adder.add(
				ButtonWidget.builder(
						            Text.translatable("mco.configure.world.delete.button"),
						            button -> client.setScreen(
								            RealmsPopups.createContinuableWarningPopup(
										            screen,
										            Text.translatable("mco.configure.world.delete.question.line1"),
										            popupScreen -> this.onDeletionConfirmed()
								            )
						            )
				            )
				            .dimensions(0, 0, 200, 20)
				            .build()
		);
		adder.add(EmptyWidget.ofHeight(2));
		this.subscriptionInfoTextWidget = adder.add(
				NarratedMultilineTextWidget.builder(Text.empty(), textRenderer).width(200).build(),
				Positioner.create().alignHorizontalCenter()
		);
		this.subscriptionInfoTextWidget.setCentered(false);
		this.update(server);
	}

	private void onDeletionConfirmed() {
		RealmsUtil.runAsync(
				          client -> client.deleteWorld(this.serverData.id),
				          RealmsUtil.openingScreenAndLogging(this.screen::createErrorScreen, "Couldn't delete world")
		          )
		          .thenRunAsync(() -> this.client.setScreen(this.screen.getParent()), this.client);
		this.client.setScreen(this.screen);
	}

	private void getSubscription(long worldId) {
		RealmsClient realmsClient = RealmsClient.create();

		try {
			Subscription subscription = realmsClient.subscriptionFor(worldId);
			this.daysLeft = this.daysLeftPresentation(subscription.daysLeft());
			this.startDate = localPresentation(subscription.startDate());
			this.type = subscription.type();
		}
		catch (RealmsServiceException var5) {
			LOGGER.error("Couldn't get subscription", var5);
			this.client.setScreen(this.screen.createErrorScreen(var5));
		}
	}

	private static Text localPresentation(Instant time) {
		String
				string =
				ZonedDateTime
						.ofInstant(time, ZoneId.systemDefault())
						.format(Util.getDefaultLocaleFormatter(FormatStyle.MEDIUM));
		return Text.literal(string).formatted(Formatting.GRAY);
	}

	private Text daysLeftPresentation(int daysLeft) {
		if (daysLeft < 0 && this.serverData.expired) {
			return EXPIRED_TEXT;
		}
		else if (daysLeft <= 1) {
			return EXPIRES_IN_LESS_THAN_A_DAY_TEXT;
		}
		else {
			int i = daysLeft / 30;
			int j = daysLeft % 30;
			boolean bl = i > 0;
			boolean bl2 = j > 0;
			if (bl && bl2) {
				return Text
						.translatable("mco.configure.world.subscription.remaining.months.days", i, j)
						.formatted(Formatting.GRAY);
			}
			else if (bl) {
				return Text
						.translatable("mco.configure.world.subscription.remaining.months", i)
						.formatted(Formatting.GRAY);
			}
			else {
				return bl2 ? Text
				             .translatable("mco.configure.world.subscription.remaining.days", j)
				             .formatted(Formatting.GRAY) : Text.empty();
			}
		}
	}

	@Override
	public void update(RealmsServer server) {
		this.serverData = server;
		this.getSubscription(server.id);
		this.startDateTextWidget.setMessage(this.startDate);
		if (this.type == Subscription.SubscriptionType.NORMAL) {
			this.timeLeftLabelTextWidget.setMessage(TIME_LEFT_LABEL_TEXT);
		}
		else if (this.type == Subscription.SubscriptionType.RECURRING) {
			this.timeLeftLabelTextWidget.setMessage(DAYS_LEFT_LABEL_TEXT);
		}

		this.daysLeftTextWidget.setMessage(this.daysLeft);
		boolean bl = RealmsMainScreen.isSnapshotRealmsEligible() && server.parentWorldName != null;
		this.deleteWorldButton.active = server.expired;
		if (bl) {
			this.subscriptionInfoTextWidget.setMessage(Text.translatable(
					"mco.snapshot.subscription.info",
					server.parentWorldName
			));
		}
		else {
			this.subscriptionInfoTextWidget.setMessage(RECURRING_INFO_TEXT);
		}

		this.grid.refreshPositions();
	}

	@Override
	public Text getNarratedHint() {
		return ScreenTexts.joinLines(
				SUBSCRIPTION_TITLE,
				SUBSCRIPTION_START_LABEL_TEXT,
				this.startDate,
				TIME_LEFT_LABEL_TEXT,
				this.daysLeft
		);
	}
}
