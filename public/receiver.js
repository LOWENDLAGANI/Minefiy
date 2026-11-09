const context = cast.framework.CastReceiverContext.getInstance();
const playerManager = context.getPlayerManager();

playerManager.setMessageInterceptor(
    cast.framework.messages.MessageType.LOAD,
    request => {
        // Here you can intercept the load request and modify it if needed
        return request;
    }
);

context.start();