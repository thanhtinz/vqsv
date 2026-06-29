/// Centralized client configuration.
///
/// Change [serverHost] in this one place to point the client at a different
/// server (e.g. a LAN IP or a deployed host). Default is 'localhost'.
class AppConfig {
  static const String serverHost = 'localhost';
  static const int tcpPort = 9090;
  static const int restPort = 8080;

  static String get restBaseUrl => 'http://$serverHost:$restPort';
}
