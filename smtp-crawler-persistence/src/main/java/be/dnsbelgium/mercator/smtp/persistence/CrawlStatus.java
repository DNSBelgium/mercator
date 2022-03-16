package be.dnsbelgium.mercator.smtp.persistence;

public class CrawlStatus {

    // See https://dmap.sidnlabs.nl/datamodel.html

    // TODO [codereview] Why integers ?
    public final static int OK                  = 0;
    public final static int MALFORMED_URL       = 1;
    public final static int TIME_OUT            = 2;
    public final static int UNKNOWN_HOST        = 3;
    public final static int NETWORK_ERROR       = 4;
    public final static int CONNECTION_REFUSED  = 9;
    public final static int PROTOCOL_ERROR      = 10;
    public final static int INVALID_HOSTNAME    = 32;


    public final static int NO_IP_ADDRESS       = 15;
    public final static int INTERNAL_ERROR      = 99;

}
