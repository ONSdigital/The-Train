package com.github.onsdigital.thetrain.helpers;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.client.SlackClientImpl;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.thetrain.configuration.AppConfiguration;

import java.util.HashMap;

import static com.github.onsdigital.thetrain.logging.TrainEvent.error;

public class Slack {
    // ToDo - only sends messages with Warning Colour

    private static Profile profile;
    private static SlackClient slack;

    static {
        // ToDo - if unchecked exception is raised here, what happens?
        profile = new Profile.Builder()
                .username(System.getenv(AppConfiguration.ARCHIVED_TRANSACTIONS_SLACK_USER_NAME_ENV_VAR))
                .emoji(":chart_with_upwards_trend:")
                .authToken(System.getenv(AppConfiguration.ARCHIVED_TRANSACTIONS_SLACK_KEY_ENV_VAR))
                .create();
                slack = new SlackClientImpl(profile);
    }

    private static void send(PostMessage m){
        PostMessageResponse response = slack.sendMessage(m);
        if (!response.isOk()){
            error().log("error occurred when attempting to post slack message. Error:"+response.toString());
        }
    }

    public static void sendSlackArchivalReasons(HashMap<String, Integer> reasons, String messageText) {
        PostMessage msg;
        if (reasons.size()>0) {
            msg = profile.newPostMessage(System.getenv(AppConfiguration.ARCHIVED_TRANSACTIONS_SLACK_CHANNEL_ENV_VAR),
                messageText);
        } else {
            msg = profile.newPostMessage(System.getenv(AppConfiguration.ARCHIVED_TRANSACTIONS_SLACK_CHANNEL_ENV_VAR),
                    messageText);
            msg.addAttachment(new PostMessageAttachment("Summary", "No transactions archived", Colour.GOOD));
        }
        for (String header : reasons.keySet()) {
            Integer tally = reasons.get(header);
            msg.addAttachment(new PostMessageAttachment(header, Integer.toString(tally), Colour.WARNING));
        }
        send(msg);
    }
}
