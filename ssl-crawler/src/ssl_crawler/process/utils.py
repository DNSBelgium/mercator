from sslyze import ScanCommand

# Commented commands are not currently used but are available in SSLyze
all_commands = {ScanCommand.CERTIFICATE_INFO,
                # ScanCommand.SESSION_RESUMPTION,
                # ScanCommand.TLS_COMPRESSION,
                # ScanCommand.TLS_1_3_EARLY_DATA,
                # ScanCommand.OPENSSL_CCS_INJECTION,
                # ScanCommand.TLS_FALLBACK_SCSV,
                # ScanCommand.HEARTBLEED,
                # ScanCommand.ROBOT,
                # ScanCommand.SESSION_RENEGOTIATION,
                ScanCommand.ELLIPTIC_CURVES,
                ScanCommand.SSL_2_0_CIPHER_SUITES,
                ScanCommand.SSL_3_0_CIPHER_SUITES,
                ScanCommand.TLS_1_0_CIPHER_SUITES,
                ScanCommand.TLS_1_1_CIPHER_SUITES,
                ScanCommand.TLS_1_2_CIPHER_SUITES,
                ScanCommand.TLS_1_3_CIPHER_SUITES}
