import 'dart:io';
import 'dart:typed_data';
import 'dart:convert';

class TcpService {
  Socket? _socket;
  final _buffer = BytesBuilder();
  Function(int opcode, Uint8List payload)? onPacket;

  Future<void> connect(String host, int port) async {
    _socket = await Socket.connect(host, port);
    _socket!.listen(
      _onData,
      onError: (Object e) => print('TCP error: $e'),
      onDone: () => print('TCP closed'),
    );
  }

  void _onData(List<int> data) {
    _buffer.add(data);
    _processBuffer();
  }

  void _processBuffer() {
    while (true) {
      final bytes = _buffer.toBytes();
      if (bytes.length < 2) {
        _buffer.clear();
        _buffer.add(bytes);
        return;
      }
      final len = ((bytes[0] << 8) | bytes[1]);
      if (bytes.length < 2 + len) {
        _buffer.clear();
        _buffer.add(bytes);
        return;
      }
      final payload = Uint8List.fromList(bytes.sublist(2, 2 + len));
      _buffer.clear();
      if (bytes.length > 2 + len) {
        _buffer.add(bytes.sublist(2 + len));
      }
      if (payload.isNotEmpty) {
        onPacket?.call(payload[0] & 0xFF, Uint8List.fromList(payload.sublist(1)));
      }
    }
  }

  void _send(Uint8List payload) {
    if (_socket == null) return;
    final header = ByteData(2);
    header.setUint16(0, payload.length, Endian.big);
    _socket!.add(header.buffer.asUint8List());
    _socket!.add(payload);
  }

  void sendLogin(String username, String password) {
    final u = utf8.encode(username);
    final p = utf8.encode(password);
    final all = Uint8List(1 + 2 + u.length + 2 + p.length);
    int i = 0;
    all[i++] = 0x01;
    all[i++] = (u.length >> 8) & 0xFF;
    all[i++] = u.length & 0xFF;
    all.setRange(i, i + u.length, u);
    i += u.length;
    all[i++] = (p.length >> 8) & 0xFF;
    all[i++] = p.length & 0xFF;
    all.setRange(i, i + p.length, p);
    _send(all);
  }

  void sendMove(int dir) => _send(Uint8List.fromList([0x02, dir]));

  void sendBattleAct(String battleId, int action, [int? itemId]) {
    final bid = utf8.encode(battleId);
    final extra = itemId != null ? 2 : 0;
    final all = Uint8List(1 + 2 + bid.length + 1 + extra);
    int i = 0;
    all[i++] = 0x03;
    all[i++] = (bid.length >> 8) & 0xFF;
    all[i++] = bid.length & 0xFF;
    all.setRange(i, i + bid.length, bid);
    i += bid.length;
    all[i++] = action;
    if (itemId != null) {
      all[i++] = (itemId >> 8) & 0xFF;
      all[i] = itemId & 0xFF;
    }
    _send(all);
  }

  void sendChat(String text) {
    final trimmed = text.length > 128 ? text.substring(0, 128) : text;
    final t = utf8.encode(trimmed);
    final all = Uint8List(1 + 2 + t.length);
    all[0] = 0x04;
    all[1] = (t.length >> 8) & 0xFF;
    all[2] = t.length & 0xFF;
    all.setRange(3, 3 + t.length, t);
    _send(all);
  }

  void sendPing() => _send(Uint8List.fromList([0x05]));

  void disconnect() {
    _socket?.destroy();
    _socket = null;
  }
}
