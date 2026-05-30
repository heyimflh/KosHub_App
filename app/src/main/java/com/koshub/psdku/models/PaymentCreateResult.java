package com.koshub.psdku.models;

import java.io.Serializable;

public class PaymentCreateResult implements Serializable {
    private boolean success;
    private String paymentId;
    private String bookingId;
    private long gatewayTransactionId;
    private double totalBayar;
    private String qrisString;
    private long expiredAt;

    public PaymentCreateResult() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public long getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(long gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }

    public double getTotalBayar() { return totalBayar; }
    public void setTotalBayar(double totalBayar) { this.totalBayar = totalBayar; }

    public String getQrisString() { return qrisString; }
    public void setQrisString(String qrisString) { this.qrisString = qrisString; }

    public long getExpiredAt() { return expiredAt; }
    public void setExpiredAt(long expiredAt) { this.expiredAt = expiredAt; }
}
