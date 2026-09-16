import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/quran_provider.dart';
import '../../data/models/surah_data.dart';
import 'surah_reader_screen.dart';

class QuranHubScreen extends StatelessWidget {
  const QuranHubScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Quran Hub', style: TextStyle(fontWeight: FontWeight.bold)),
          backgroundColor: Theme.of(context).colorScheme.background,
          bottom: const TabBar(
            tabs: [
              Tab(text: "Recitation Tracker"),
              Tab(text: "Recite Quran"),
            ],
            indicatorColor: Colors.teal,
            labelColor: Colors.teal,
          ),
        ),
        body: const TabBarView(
          children: [
            _RecitationTrackerTab(),
            _ReciteQuranTab(),
          ],
        ),
      ),
    );
  }
}

class _RecitationTrackerTab extends StatelessWidget {
  const _RecitationTrackerTab();

  @override
  Widget build(BuildContext context) {
    return Consumer<QuranProvider>(
      builder: (context, provider, child) {
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Card(
              color: Colors.teal.withOpacity(0.2),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text("Overall Quran Progress", style: TextStyle(fontWeight: FontWeight.bold)),
                    Text("Page ${provider.currentReadPage} / 604", style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    LinearProgressIndicator(
                      value: provider.overallProgressPercentage / 100,
                      backgroundColor: Colors.grey[800],
                      color: Colors.teal,
                    ),
                    const SizedBox(height: 4),
                    Text("${provider.overallProgressPercentage.toStringAsFixed(1)}% Completed", style: const TextStyle(fontSize: 12)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("Juz ${provider.currentJuzState.juzNumber}", style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    Text("Page range: ${provider.currentJuzState.startPage} - ${provider.currentJuzState.endPage}", style: const TextStyle(color: Colors.grey)),
                    const SizedBox(height: 16),
                    LinearProgressIndicator(
                      value: provider.currentJuzState.completionPercentage / 100,
                      backgroundColor: Colors.grey[800],
                      color: Colors.teal,
                    ),
                    const SizedBox(height: 4),
                    Text("${provider.currentJuzState.completionPercentage.toStringAsFixed(0)}% of Juz ${provider.currentJuzState.juzNumber} completed", style: const TextStyle(fontSize: 12)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: () {
                // Show log dialog
              },
              icon: const Icon(Icons.add),
              label: const Text("Log Recitation Session"),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.teal.withOpacity(0.2),
                foregroundColor: Colors.teal,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
            ),
            const SizedBox(height: 24),
            const Text("Recitation History", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            if (provider.recentQuranLogs.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32.0),
                  child: Text("No recitation logged yet. Record your progress to begin.", textAlign: TextAlign.center, style: TextStyle(color: Colors.grey)),
                ),
              )
            else
              ...provider.recentQuranLogs.map((log) => ListTile(
                title: Text(log.surah),
                subtitle: Text("Pages ${log.startPage} - ${log.endPage}"),
                trailing: IconButton(
                  icon: const Icon(Icons.delete, color: Colors.red),
                  onPressed: () => provider.deleteLog(log),
                ),
              )),
          ],
        );
      },
    );
  }
}

class _ReciteQuranTab extends StatelessWidget {
  const _ReciteQuranTab();

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: SurahData.surahs.length,
      itemBuilder: (context, index) {
        final surah = SurahData.surahs[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: Colors.teal.withOpacity(0.1),
              child: Text("${surah.number}", style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold)),
            ),
            title: Text(surah.nameEnglish, style: const TextStyle(fontWeight: FontWeight.bold)),
            subtitle: Text("${surah.revelationType} • ${surah.verseCount} verses"),
            trailing: Text(surah.nameArabic, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.teal)),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => SurahReaderScreen(surah: surah),
                ),
              );
            },
          ),
        );
      },
    );
  }
}
