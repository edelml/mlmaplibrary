package com.ml.map;

public class GoblobException extends Exception{
    public static final int NETWORK_ERROR = 1;
    public static final int MAX_NUMBER_PEERS = 2;
    public static final int OTHER_CAUSE = -1;
    public int code;
    private String msg;

    public GoblobException(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public GoblobException(int code){
        this.code = code;
        generateMessage();
    }

    private void generateMessage() {
        switch (code){
            case NETWORK_ERROR:
                msg = GoblobLocationManager.getInstance().getApplicationContext().getResources().getString(R.string.network_error);
                break;
        }
    }

    @Override
    public String getMessage() {
        if (msg != null && msg.length() > 0)
            return msg;
        return super.getMessage();
    }
}
