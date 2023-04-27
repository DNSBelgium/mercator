package be.dnsbelgium.mercator.content.dto;

public enum Status {
    Ok("succeeded"),
    TimeOut("Failed: Time out error"),
    HtmlTooBig("Failed: Html file too big"),
    screenshotTooBig("Failed: Screenshot file too big"),
    UploadFailed("Failed: Upload failed"),
    NameNotResolved("Failed: Name not resolved"),
    UnexpectedError("Failed: Unexpected error");

    private final String status;

    Status(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

}
