import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/game_state.dart';
import '../services/api_service.dart';
import '../services/tcp_service.dart';
import 'map_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _userCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  bool _loading = false;
  late TcpService _tcp;

  // Server host/ports are centralized in lib/config.dart (AppConfig).
  static const _serverHost = AppConfig.serverHost;
  static const _tcpPort = AppConfig.tcpPort;
  static const _restPort = AppConfig.restPort;

  @override
  void initState() {
    super.initState();
    _tcp = TcpService();
  }

  @override
  void dispose() {
    _userCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    setState(() => _loading = true);
    try {
      final api = ApiService('http://$_serverHost:$_restPort');
      final json = await api.login(_userCtrl.text.trim(), _passCtrl.text);
      if (!mounted) return;
      context.read<GameState>().updateFromAuthJson(json);
      await _tcp.connect(_serverHost, _tcpPort);
      _tcp.sendLogin(_userCtrl.text.trim(), _passCtrl.text);
      _wireAuthHandlers();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _register() async {
    setState(() => _loading = true);
    try {
      final api = ApiService('http://$_serverHost:$_restPort');
      final json = await api.register(
        _userCtrl.text.trim(),
        _passCtrl.text,
        _userCtrl.text.trim(),
      );
      if (!mounted) return;
      context.read<GameState>().updateFromAuthJson(json);
      await _tcp.connect(_serverHost, _tcpPort);
      _tcp.sendLogin(_userCtrl.text.trim(), _passCtrl.text);
      _wireAuthHandlers();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  /// Register TCP callbacks that handle the binary AUTH_OK body (and login
  /// errors). On AUTH_OK we reconcile the authoritative server fields into
  /// GameState and navigate to the map. The raw onPacket handler is left for
  /// MapScreen/BattleScreen to take over once navigation completes.
  void _wireAuthHandlers() {
    _tcp.onAuthOk = (auth) {
      if (!mounted) return;
      context.read<GameState>().applyAuthOk(
            auth.token,
            auth.level,
            auth.kimTien,
            auth.mapId,
            auth.posX,
            auth.posY,
          );
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => MapScreen(tcpService: _tcp),
        ),
      );
    };
    _tcp.onError = (msg) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(msg)));
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A25),
      body: Center(
        child: SizedBox(
          width: 400,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'VQSV',
                style: TextStyle(
                  fontSize: 36,
                  color: Colors.yellow,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const Text(
                'Vương Quốc Siêu Vật',
                style: TextStyle(fontSize: 16, color: Colors.white70),
              ),
              const SizedBox(height: 40),
              TextField(
                controller: _userCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(
                  labelText: 'Tên đăng nhập',
                  labelStyle: TextStyle(color: Colors.white54),
                  enabledBorder: OutlineInputBorder(
                    borderSide: BorderSide(color: Colors.white24),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: BorderSide(color: Colors.blue),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _passCtrl,
                obscureText: true,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(
                  labelText: 'Mật khẩu',
                  labelStyle: TextStyle(color: Colors.white54),
                  enabledBorder: OutlineInputBorder(
                    borderSide: BorderSide(color: Colors.white24),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: BorderSide(color: Colors.blue),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              if (_loading)
                const CircularProgressIndicator()
              else
                Column(
                  children: [
                    ElevatedButton(
                      onPressed: _login,
                      style: ElevatedButton.styleFrom(
                        minimumSize: const Size(200, 48),
                      ),
                      child: const Text('ĐĂNG NHẬP'),
                    ),
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: _register,
                      child: const Text(
                        'ĐĂNG KÝ',
                        style: TextStyle(color: Colors.white54),
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}
