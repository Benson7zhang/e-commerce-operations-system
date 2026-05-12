package com.emall.channel.adapter;

public class ChannelAdapterException extends RuntimeException {

    private final String channelCode;
    private final String operation;

    public ChannelAdapterException(String channelCode, String operation, String message) {
        super("[" + channelCode + "] " + operation + " failed: " + message);
        this.channelCode = channelCode;
        this.operation = operation;
    }

    public ChannelAdapterException(String channelCode, String operation, String message, Throwable cause) {
        super("[" + channelCode + "] " + operation + " failed: " + message, cause);
        this.channelCode = channelCode;
        this.operation = operation;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public String getOperation() {
        return operation;
    }
}
