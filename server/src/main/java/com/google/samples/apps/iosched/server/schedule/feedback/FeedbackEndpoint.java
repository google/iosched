package com.google.samples.apps.iosched.server.schedule.feedback;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.samples.apps.iosched.server.schedule.feedback.model.SessionFeedback;
import com.google.samples.apps.iosched.server.userdata.Ids;
import com.google.samples.apps.iosched.server.userdata.UserdataEndpoint;
import java.util.logging.Logger;

/** Endpoint for session feedback. */
@Api(
    name = "feedback",
    title = "IOSched Session Feedback",
    description = "Proxy endpoint that send feedback data to external CMS.",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "iosched.apps.samples.google.com",
        ownerName = "google.com",
        packagePath = "rpc"
    ),
    clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID,
        Ids.IOS_CLIENT_ID_DEV_IO2017,
        Ids.IOS_CLIENT_ID_DOGFOOD_IO2017,
        Ids.IOS_CLIENT_ID_GWEB_IO2017,
        Ids.IOS_CLIENT_ID_DEV_GWEB_IO2017,
        Ids.IOS_CLIENT_ID_DOGFOOD_GWEB_IO2017,
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID},
    audiences = {Ids.ANDROID_AUDIENCE}
)
public class FeedbackEndpoint {

  /**
   * Send session feedback to CMS. Session feedback is not stored within this app. This endpoint
   * is a proxy to an external CMS that actually stores the session feedback.
   *
   * @param user Current user (injected by Endpoints)
   * @param sessionFeedback Attendee feedback for a session.
   */
  @ApiMethod(name = "sendSessionFeedback", path = "send", httpMethod = ApiMethod.HttpMethod
      .POST)
  public void sendSessionFeedback(User user, SessionFeedback sessionFeedback)
      throws UnauthorizedException, BadRequestException {
    if (user == null) {
      throw new UnauthorizedException("Invalid credentials");
    }
    // TODO(arthurthompson): Remove this temporary if-else block. It is only here to test that
    // TODO                  the sessionFeedback object is being received correctly.
    if (sessionFeedback == null || sessionFeedback.getSessionId() == null ||
        sessionFeedback.getRatings().length <= 0) {
      throw new BadRequestException("Invalid session feedback");
    } else {
      Logger.getLogger(UserdataEndpoint.class.getName()).info("Feedback looks good");
    }
    // TODO(arthurthomspon): Implement sending feedback data to Event Point.
  }

}
