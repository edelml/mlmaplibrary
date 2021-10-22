package com.ml.map;

public class GoblobEvents {
    public static class Error {
        public Exception exception;

        public Error(Exception e) {
            this.exception = e;
        }
    }

    public static class NetworkConnected {
    }

    public static class NetworkDisconnected {
    }

    public static class LoginSuccessfully {

    }
}
