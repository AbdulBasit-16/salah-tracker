import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/salah_provider.dart';
import '../../data/models/qaza_counter.dart';

class InsightsScreen extends StatelessWidget {
  const InsightsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Insights & Qaza', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Theme.of(context).colorScheme.background,
      ),
      body: Consumer<SalahProvider>(
        builder: (context, provider, child) {
          final todayLogs = provider.todayPrayerLogs;
          final qazaBalances = provider.qazaBalances;
          
          final obligatoryLogs = todayLogs.where((log) => 
            ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"].contains(log.prayerName)
          ).toList();

          int offeredCount = obligatoryLogs.where((l) => l.status.startsWith("Offered")).length;
          int missedCount = obligatoryLogs.where((l) => l.status == "Missed/Qaza").length;
          int excusedCount = obligatoryLogs.where((l) => l.status == "Excused").length;
          int pendingCount = obligatoryLogs.where((l) => l.status == "Pending").length;

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              const Text("Today's Progress", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.teal)),
              const SizedBox(height: 16),
              Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Row(
                    children: [
                      // Simple progress indicator for now
                      SizedBox(
                        width: 100,
                        height: 100,
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            SizedBox(
                              width: 100,
                              height: 100,
                              child: CircularProgressIndicator(
                                value: (offeredCount + excusedCount) / 5.0,
                                strokeWidth: 10,
                                backgroundColor: Colors.grey[800],
                                color: Colors.teal,
                              ),
                            ),
                            Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text("${((offeredCount + excusedCount) / 5.0 * 100).toInt()}%", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                                const Text("Done", style: TextStyle(fontSize: 12, color: Colors.grey)),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 24),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _LegendItem(color: Colors.teal, label: "Offered: $offeredCount"),
                            const SizedBox(height: 6),
                            _LegendItem(color: Colors.blue, label: "Excused: $excusedCount"),
                            const SizedBox(height: 6),
                            _LegendItem(color: Colors.grey, label: "Missed: $missedCount"),
                            const SizedBox(height: 6),
                            _LegendItem(color: Colors.black54, label: "Pending: $pendingCount"),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              const Text("Qaza Balances", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.teal)),
              const SizedBox(height: 16),
              Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: qazaBalances.map((counter) => _QazaEditorRow(
                    counter: counter,
                    onIncrement: () {
                      // Implement increment
                    },
                    onDecrement: () {
                      // Implement decrement
                    },
                  )).toList(),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _LegendItem extends StatelessWidget {
  final Color color;
  final String label;

  const _LegendItem({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(width: 12, height: 12, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(3))),
        const SizedBox(width: 8),
        Text(label, style: const TextStyle(fontWeight: FontWeight.w500)),
      ],
    );
  }
}

class _QazaEditorRow extends StatelessWidget {
  final QazaCounter counter;
  final VoidCallback onIncrement;
  final VoidCallback onDecrement;

  const _QazaEditorRow({
    required this.counter,
    required this.onIncrement,
    required this.onDecrement,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(counter.prayerName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                Text("${counter.count} prayers owed", style: TextStyle(color: counter.count > 0 ? Colors.grey : Colors.teal, fontSize: 12)),
              ],
            ),
          ),
          Row(
            children: [
              IconButton(
                icon: const Icon(Icons.remove),
                onPressed: counter.count > 0 ? onDecrement : null,
                color: Colors.teal,
              ),
              SizedBox(
                width: 32,
                child: Text("${counter.count}", textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              ),
              IconButton(
                icon: const Icon(Icons.add),
                onPressed: onIncrement,
                color: Colors.teal,
              ),
            ],
          ),
        ],
      ),
    );
  }
}
