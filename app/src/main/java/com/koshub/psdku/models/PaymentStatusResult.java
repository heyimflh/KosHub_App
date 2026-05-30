package com.koshub.psdku.models;

import java.io.Serializable;

public class PaymentStatusResult implements Serializable {
    private boolean success;
    private String status;
    private String message;

    public PaymentStatusResult() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
