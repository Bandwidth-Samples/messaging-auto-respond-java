package com.bandwidth.controller;

import com.bandwidth.Main;
import com.bandwidth.sdk.ApiClient;
import com.bandwidth.sdk.ApiResponse;
import com.bandwidth.sdk.ApiException;
import com.bandwidth.sdk.ApiClient;
import com.bandwidth.sdk.auth.HttpBasicAuth;
import com.bandwidth.sdk.Configuration;
import com.bandwidth.sdk.model.*;
import com.bandwidth.sdk.api.MessagesApi;
import com.bandwidth.model.CreateMessage;
import com.bandwidth.model.MessageReply;
import com.bandwidth.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("callbacks")
public class CallbacksController {

    Logger logger = LoggerFactory.getLogger(CallbacksController.class);

    private String username = System.getenv("BW_USERNAME");
    private String password = System.getenv("BW_PASSWORD");
    private String accountId = System.getenv("BW_ACCOUNT_ID");
    private String bwNumber = System.getenv("BW_NUMBER");
    private String applicationId = System.getenv("BW_MESSAGING_APPLICATION_ID");

    ApiClient defaultClient = Configuration.getDefaultApiClient();
    HttpBasicAuth Basic = (HttpBasicAuth) defaultClient.getAuthentication("Basic");
    private final MessagesApi api = new MessagesApi(defaultClient);
    public MessageRequest messageRequest = new MessageRequest();
    public MessageReply messageReply = new MessageReply();


    @RequestMapping("/outbound/messaging/status")
    public void statusCallback(@RequestBody InboundMessageCallback[] callbacks) {

        for (InboundMessageCallback callback : callbacks) {
            logger.info(callback.getType());
            logger.info(callback.getDescription());
            switch (callback.getType()) {
                case "message-sending":
                    logger.info("message-sending type is only for MMS");
                    break;
                case "message-delivered":
                    logger.info("your message has been handed off to the Bandwidth's MMSC network, but has not been confirmed at the downstream carrier");
                    break;
                case "message-failed":
                    logger.info("For MMS and Group Messages, you will only receive this callback if you have enabled delivery receipts on MMS. ");
                    break;
                default:
                    logger.info("Message type does not match endpoint. This endpoint is used for message status callbacks only.");
                    break;
            }
        }
    }

    @RequestMapping("/inbound/messaging")
    public void inboundCallback(@RequestBody InboundMessageCallback[] callbacks) {

        Basic.setUsername(username);
        Basic.setPassword(password);
        messageRequest.applicationId(applicationId);
        messageRequest.priority(PriorityEnum.DEFAULT);
        messageRequest.from(bwNumber);


	for (InboundMessageCallback callback : callbacks) {
            logger.info(callback.getType());
            logger.info(callback.getDescription());

            messageRequest.addToItem(callback.getMessage().getFrom());

            logger.info(callback.getMessage().getText());
            switch (callback.getMessage().getText().toLowerCase()) {
	    case "stop":
                    messageRequest.text("STOP: OK, you'll no longer receive messages from us.");
                    break;
                case "quit":
                    messageRequest.text("QUIT: OK, you'll no longer receive messages from us.");
                    break;
                case "info":
                    messageRequest.text("INFO: This is the test responder service. Reply STOP or QUIT to opt out.");
                    break;
                case "help":
                    messageRequest.text("HELP: This is the test responder service. Reply STOP or QUIT to opt out.");
                    break;
                default:
                    messageRequest.text("AUTO-REPLY: Thank you for your message! Please respond with a valid word. Reply HELP for help.");
                    break;
            }
            try {
                Message response = api.createMessage(accountId, messageRequest);
                messageReply.setSuccess(true);
            } catch (ApiException e) { // Bandwidth API response status not 2XX
                messageReply.setSuccess(false);
                messageReply.setError(e.getMessage());
            }
	}
    }

}
