import 'dart:typed_data';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/game_state.dart';
import '../services/tcp_service.dart';

class BattleScreen extends StatefulWidget {
  final TcpService tcpService;
  const BattleScreen({super.key, required this.tcpService});

  @override
  State<BattleScreen> createState() => _BattleScreenState();
}

class _BattleScreenState extends State<BattleScreen> {
  bool _waiting = false;

  static const _endStatuses = {'VICTORY', 'DEFEAT', 'ESCAPED', 'CAUGHT'};

  @override
  void initState() {
    super.initState();
    widget.tcpService.onPacket = _handlePacket;
  }

  void _handlePacket(int op, Uint8List payload) {
    if (!mounted) return;
    if (op == 0x85) {
      // BATTLE_TURN: [2B pHp][2B eHp][1B sLen][status][2B logLen][log]
      int offset = 0;
      if (payload.length < 4) return;
      final pHp = (payload[offset] << 8) | payload[offset + 1];
      offset += 2;
      final eHp = (payload[offset] << 8) | payload[offset + 1];
      offset += 2;
      if (payload.length < offset + 1) return;
      final sLen = payload[offset];
      offset += 1;
      if (payload.length < offset + sLen) return;
      final status = utf8.decode(payload.sublist(offset, offset + sLen));
      offset += sLen;
      if (payload.length < offset + 2) return;
      final logLen = (payload[offset] << 8) | payload[offset + 1];
      offset += 2;
      if (payload.length < offset + logLen) return;
      final log = utf8.decode(payload.sublist(offset, offset + logLen));

      context.read<GameState>().updateBattle(pHp, eHp, status, log);
      setState(() => _waiting = false);

      if (_endStatuses.contains(status.toUpperCase())) {
        context.read<GameState>().clearBattle();
        if (mounted) Navigator.pop(context);
      }
    }
  }

  void _sendAction(int action, GameState gs) {
    final battleId = gs.activeBattleId;
    if (battleId == null) return;
    widget.tcpService.sendBattleAct(battleId, action);
    setState(() => _waiting = true);
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<GameState>(
      builder: (context, gs, _) {
        final maxEnemyHp = gs.battleEnemyMaxHp > 0 ? gs.battleEnemyMaxHp : 1;
        final maxPlayerHp =
            (gs.player?.hpMax ?? 1) > 0 ? (gs.player?.hpMax ?? 1) : 1;
        final logEntries = gs.battleLog.reversed.take(5).toList().reversed.toList();

        return Scaffold(
          backgroundColor: const Color(0xFF0A0A25),
          appBar: AppBar(
            backgroundColor: const Color(0xFF1A1A3A),
            title: Text(
              'Chế́n đấu: ${gs.battleEnemyName} Lv.${gs.battleEnemyLevel}',
              style: const TextStyle(fontSize: 15, color: Colors.white),
            ),
            automaticallyImplyLeading: false,
          ),
          body: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                // Enemy info
                Text(
                  '${gs.battleEnemyName}  Lv.${gs.battleEnemyLevel}',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                // Enemy HP bar
                Row(
                  children: [
                    const Text(
                      'HP: ',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    Expanded(
                      child: LinearProgressIndicator(
                        value: gs.battleEnemyHp / maxEnemyHp,
                        backgroundColor: Colors.white24,
                        color: Colors.red,
                        minHeight: 12,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '${gs.battleEnemyHp}',
                      style: const TextStyle(
                          color: Colors.white70, fontSize: 13),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                // Battle log
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFF1A1A3A),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    padding: const EdgeInsets.all(8),
                    child: ListView.builder(
                      itemCount: logEntries.length,
                      itemBuilder: (_, i) => Text(
                        logEntries[i],
                        style: const TextStyle(
                            color: Colors.white70, fontSize: 13),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                // Player HP bar
                Row(
                  children: [
                    const Text(
                      'HP bạn: ',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    Expanded(
                      child: LinearProgressIndicator(
                        value: gs.battlePlayerHp / maxPlayerHp,
                        backgroundColor: Colors.white24,
                        color: Colors.green,
                        minHeight: 12,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '${gs.battlePlayerHp}/${gs.player?.hpMax ?? 0}',
                      style: const TextStyle(
                          color: Colors.white70, fontSize: 13),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                // Action buttons 2x2
                GridView.count(
                  crossAxisCount: 2,
                  shrinkWrap: true,
                  mainAxisSpacing: 8,
                  crossAxisSpacing: 8,
                  childAspectRatio: 3,
                  physics: const NeverScrollableScrollPhysics(),
                  children: [
                    _actionButton(
                      label: 'Tấn công',
                      action: 0,
                      gs: gs,
                      color: Colors.red,
                    ),
                    _actionButton(
                      label: 'Dùng đồ',
                      action: 1,
                      gs: gs,
                      color: Colors.orange,
                    ),
                    _actionButton(
                      label: 'Bắt thú',
                      action: 2,
                      gs: gs,
                      color: Colors.blue,
                      disabled: !gs.catchable,
                    ),
                    _actionButton(
                      label: 'Bỏ chạy',
                      action: 3,
                      gs: gs,
                      color: Colors.grey,
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _actionButton({
    required String label,
    required int action,
    required GameState gs,
    required Color color,
    bool disabled = false,
  }) {
    final isDisabled = _waiting || disabled;
    return ElevatedButton(
      onPressed: isDisabled ? null : () => _sendAction(action, gs),
      style: ElevatedButton.styleFrom(
        backgroundColor: isDisabled ? Colors.grey.shade800 : color,
      ),
      child: Text(
        label,
        style: const TextStyle(fontWeight: FontWeight.bold),
      ),
    );
  }
}
