import os
import glob

kt_files = glob.glob('app/src/main/java/com/rtekmidev/marijancbt/*.kt')

snippet = """
        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }
"""

for file in kt_files:
    if "RekapActivity.kt" in file:
        continue # Skip because we did it manually

    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    if "WindowCompat.setDecorFitsSystemWindows(window, false)" in content and "setContentView(R.layout." in content:
        if "Terapkan bottom inset secara global" not in content:
            # Cari setContentView
            lines = content.split('\n')
            new_lines = []
            for line in lines:
                new_lines.append(line)
                if "setContentView(R.layout." in line:
                    new_lines.append(snippet)
            
            with open(file, 'w', encoding='utf-8') as f:
                f.write('\n'.join(new_lines))
            print(f"Patched {file}")
