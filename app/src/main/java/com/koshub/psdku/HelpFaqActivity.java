package com.koshub.psdku;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class HelpFaqActivity extends AppCompatActivity {

    private LinearLayout faqContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Edge-to-edge support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_help_faq);

        faqContainer = findViewById(R.id.faqContainer);
        View navbarHelp = findViewById(R.id.navbarHelp);
        findViewById(R.id.btnBackHelp).setOnClickListener(v -> finish());

        // Handle Safe Area Insets
        ViewCompat.setOnApplyWindowInsetsListener(navbarHelp, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        findViewById(R.id.btnTanyaAsisten).setOnClickListener(v -> {
            Intent intent = new Intent(HelpFaqActivity.this, AiAssistantActivity.class);
            startActivity(intent);
        });

        setupFaqItems();
        setupContactButtons();
    }

    private void setupFaqItems() {
        addFaqItem("Bagaimana cara booking kos?", 
            "Cari kos yang Anda inginkan di halaman Home atau Map, klik 'Lihat Detail', lalu tekan tombol 'Booking Sekarang'. Pastikan data diri Anda sudah lengkap.");
        
        addFaqItem("Apakah booking bisa dibatalkan?", 
            "Ya, Anda dapat membatalkan booking selama statusnya masih 'Pending'. Jika sudah disetujui atau dibayar, silakan hubungi pemilik kos atau CS untuk bantuan lebih lanjut.");
        
        addFaqItem("Bagaimana cara menghubungi pemilik kos?", 
            "Setelah melakukan booking, Anda dapat menggunakan fitur 'Chat' di aplikasi untuk berkomunikasi langsung dengan pemilik kos.");
        
        addFaqItem("Apa itu Waiting List?", 
            "Waiting List adalah fitur untuk mengantre jika kamar kos yang Anda inginkan saat ini sedang penuh. Kami akan memberi tahu Anda jika ada kamar yang tersedia.");
        
        addFaqItem("Bagaimana cara memberi ulasan?", 
            "Ulasan dapat diberikan setelah masa sewa Anda selesai atau booking dinyatakan 'Completed' melalui menu Riwayat di halaman profil.");
    }

    private void addFaqItem(String question, String answer) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setBackgroundResource(R.drawable.bg_owner_card);
        itemLayout.setElevation(2);
        itemLayout.setPadding(32, 32, 32, 32);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        itemLayout.setLayoutParams(params);

        TextView qText = new TextView(this);
        qText.setText(question);
        qText.setTextSize(14);
        qText.setTextColor(getResources().getColor(R.color.home_text_dark));
        qText.setTypeface(null, android.graphics.Typeface.BOLD);
        qText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_profile_chevron_right, 0);
        qText.setCompoundDrawablePadding(16);

        TextView aText = new TextView(this);
        aText.setText(answer);
        aText.setTextSize(13);
        aText.setTextColor(getResources().getColor(R.color.home_text_secondary));
        aText.setPadding(0, 16, 0, 0);
        aText.setVisibility(View.GONE);

        itemLayout.addView(qText);
        itemLayout.addView(aText);

        itemLayout.setOnClickListener(v -> {
            if (aText.getVisibility() == View.GONE) {
                aText.setVisibility(View.VISIBLE);
            } else {
                aText.setVisibility(View.GONE);
            }
        });

        faqContainer.addView(itemLayout);
    }

    private void setupContactButtons() {
        findViewById(R.id.btnContactWA).setOnClickListener(v -> {
            String url = getString(R.string.cs_whatsapp_url);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        findViewById(R.id.btnContactEmail).setOnClickListener(v -> {
            String email = getString(R.string.cs_email_address);
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Bantuan KosHub");
            startActivity(Intent.createChooser(intent, "Kirim Email"));
        });
    }
}
