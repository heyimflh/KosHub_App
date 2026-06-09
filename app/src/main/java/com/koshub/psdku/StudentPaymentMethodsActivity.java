package com.koshub.psdku;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.koshub.psdku.adapters.PaymentMethodAdapter;
import com.koshub.psdku.models.PaymentMethod;
import com.koshub.psdku.repositories.PaymentMethodRepository;
import com.koshub.psdku.utils.DatabaseConstants;

import java.util.ArrayList;
import java.util.List;

public class StudentPaymentMethodsActivity extends AppCompatActivity {

    private RecyclerView rvMethods;
    private PaymentMethodAdapter adapter;
    private List<PaymentMethod> methodList = new ArrayList<>();
    private ListenerRegistration methodListener;
    private String currentUserId;

    private View cardDefault;
    private TextView tvDefaultProvider, tvDefaultAccount;
    private ImageView ivDefaultIcon;
    private LinearLayout layoutEmpty;
    private View btnAddMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_payment_methods);

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        initViews();
        applyPaymentMethodsInsets();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvMethods = findViewById(R.id.rvPaymentMethods);
        cardDefault = findViewById(R.id.cardDefaultMethod);
        tvDefaultProvider = findViewById(R.id.tvDefaultProvider);
        tvDefaultAccount = findViewById(R.id.tvDefaultAccount);
        ivDefaultIcon = findViewById(R.id.ivDefaultIcon);
        layoutEmpty = findViewById(R.id.layoutEmptyMethods);

        btnAddMethod = findViewById(R.id.btnAddMethod);
        btnAddMethod.setOnClickListener(v -> showAddEditSheet(null));
    }

    private void applyPaymentMethodsInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        View root = findViewById(R.id.rootStudentPaymentMethods);
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        View scroll = findViewById(R.id.scrollPaymentMethods);

        final int scrollLeft = scroll.getPaddingLeft();
        final int scrollTop = scroll.getPaddingTop();
        final int scrollRight = scroll.getPaddingRight();
        final int scrollBottom = scroll.getPaddingBottom();

        final ViewGroup.MarginLayoutParams buttonParams =
                (ViewGroup.MarginLayoutParams) btnAddMethod.getLayoutParams();
        final int buttonBaseBottomMargin = buttonParams.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams spacerParams = statusBarSpacer.getLayoutParams();
            if (spacerParams.height != bars.top) {
                spacerParams.height = bars.top;
                statusBarSpacer.setLayoutParams(spacerParams);
            }

            scroll.setPadding(
                    scrollLeft,
                    scrollTop,
                    scrollRight,
                    scrollBottom
            );

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) btnAddMethod.getLayoutParams();
            int targetBottomMargin = buttonBaseBottomMargin + bars.bottom;
            if (params.bottomMargin != targetBottomMargin) {
                params.bottomMargin = targetBottomMargin;
                btnAddMethod.setLayoutParams(params);
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    private void setupRecyclerView() {
        adapter = new PaymentMethodAdapter(methodList, new PaymentMethodAdapter.OnMethodClickListener() {
            @Override
            public void onMethodClick(PaymentMethod method) {
                // Optional: set as default or view detail
            }

            @Override
            public void onMenuClick(View view, PaymentMethod method) {
                showMenu(view, method);
            }
        });
        rvMethods.setLayoutManager(new LinearLayoutManager(this));
        rvMethods.setAdapter(adapter);
    }

    private void loadData() {
        PaymentMethodRepository.getInstance().ensureDefaultQrisMethod(currentUserId, new PaymentMethodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                listenToMethods();
            }

            @Override
            public void onError(String message) {
                if (message.contains("Akses database belum diizinkan") || message.contains("PERMISSION_DENIED")) {
                    showToast("Akses database belum diizinkan. Periksa Firestore Rules.");
                } else {
                    showToast(message);
                }
                listenToMethods(); // Still listen to show empty state if permission denied
            }
        });
    }

    private void listenToMethods() {
        if (methodListener != null) methodListener.remove();
        methodListener = PaymentMethodRepository.getInstance().listenPaymentMethods(currentUserId, new PaymentMethodRepository.PaymentMethodListCallback() {
            @Override
            public void onSuccess(List<PaymentMethod> methods) {
                methodList = methods;
                updateUI();
            }

            @Override
            public void onError(String message) {
                if (message.contains("Akses ditolak") || message.contains("PERMISSION_DENIED")) {
                    showToast("Akses database belum diizinkan. Periksa Firestore Rules.");
                } else {
                    showToast(message);
                }
                updateUI(); // Force update UI to show empty state correctly
            }
        });
    }

    private void updateUI() {
        adapter.updateList(methodList);
        
        PaymentMethod defaultMethod = null;
        for (PaymentMethod m : methodList) {
            if (m.isDefault()) {
                defaultMethod = m;
                break;
            }
        }

        if (defaultMethod != null) {
            cardDefault.setVisibility(View.VISIBLE);
            tvDefaultProvider.setText(defaultMethod.getProviderName());
            String acc = defaultMethod.getAccountName();
            if (defaultMethod.getMaskedNumber() != null && !defaultMethod.getMaskedNumber().isEmpty()) {
                acc += " • " + defaultMethod.getMaskedNumber();
            }
            tvDefaultAccount.setText(acc);
            
            if (DatabaseConstants.PAYMENT_METHOD_QRIS.equals(defaultMethod.getType())) {
                ivDefaultIcon.setImageResource(R.drawable.ic_payment);
            } else {
                ivDefaultIcon.setImageResource(R.drawable.ic_finance_wallet);
            }
        } else {
            cardDefault.setVisibility(View.GONE);
        }

        layoutEmpty.setVisibility(methodList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showMenu(View v, PaymentMethod method) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Jadikan Utama");
        popup.getMenu().add("Edit");
        popup.getMenu().add("Hapus");
        
        popup.setOnMenuItemClickListener(item -> {
            if ("Jadikan Utama".equals(item.getTitle())) {
                PaymentMethodRepository.getInstance().setDefaultMethod(currentUserId, method.getId(), new SimpleRepoCallback());
            } else if ("Edit".equals(item.getTitle())) {
                showAddEditSheet(method);
            } else if ("Hapus".equals(item.getTitle())) {
                PaymentMethodRepository.getInstance().deletePaymentMethod(method.getId(), new SimpleRepoCallback());
            }
            return true;
        });
        popup.show();
    }

    private void showAddEditSheet(PaymentMethod existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add_payment_method, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        Spinner spType = view.findViewById(R.id.spMethodType);
        Spinner spProvider = view.findViewById(R.id.spProvider);
        EditText etName = view.findViewById(R.id.etAccountName);
        EditText etNumber = view.findViewById(R.id.etAccountNumber);
        CheckBox cbDefault = view.findViewById(R.id.cbIsDefault);
        View btnSave = view.findViewById(R.id.btnSaveMethod);

        if (existing != null) {
            tvTitle.setText("Edit Metode Pembayaran");
        }

        // Setup Spinners
        String[] types = {"QRIS", "Bank Transfer", "E-Wallet"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(typeAdapter);

        List<String> providers = new ArrayList<>();
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, providers);
        spProvider.setAdapter(providerAdapter);

        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                providers.clear();
                if (position == 0) { // QRIS
                    providers.add("QRIS");
                    etName.setVisibility(View.GONE);
                    etNumber.setVisibility(View.GONE);
                    view.findViewById(R.id.tvLabelAccountName).setVisibility(View.GONE);
                    view.findViewById(R.id.tvLabelAccountNumber).setVisibility(View.GONE);
                } else if (position == 1) { // Bank
                    providers.add("BRI"); providers.add("BCA"); providers.add("BNI"); providers.add("Mandiri"); providers.add("BTN"); providers.add("Lainnya");
                    etName.setVisibility(View.VISIBLE);
                    etNumber.setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tvLabelAccountName).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tvLabelAccountNumber).setVisibility(View.VISIBLE);
                } else { // E-Wallet
                    providers.add("DANA"); providers.add("OVO"); providers.add("GoPay"); providers.add("ShopeePay"); providers.add("LinkAja");
                    etName.setVisibility(View.VISIBLE);
                    etNumber.setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tvLabelAccountName).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tvLabelAccountNumber).setVisibility(View.VISIBLE);
                }
                providerAdapter.notifyDataSetChanged();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (existing != null) {
            etName.setText(existing.getAccountName());
            etNumber.setText(existing.getAccountNumber());
            cbDefault.setChecked(existing.isDefault());
            // Pre-select spinners...
            if (DatabaseConstants.PAYMENT_METHOD_BANK.equals(existing.getType())) spType.setSelection(1);
            else if (DatabaseConstants.PAYMENT_METHOD_EWALLET.equals(existing.getType())) spType.setSelection(2);
        }

        btnSave.setOnClickListener(v -> {
            int typePos = spType.getSelectedItemPosition();
            String type = DatabaseConstants.PAYMENT_METHOD_QRIS;
            if (typePos == 1) type = DatabaseConstants.PAYMENT_METHOD_BANK;
            else if (typePos == 2) type = DatabaseConstants.PAYMENT_METHOD_EWALLET;

            String provider = spProvider.getSelectedItem().toString();
            String name = etName.getText().toString().trim();
            String number = etNumber.getText().toString().trim();
            boolean isDef = cbDefault.isChecked();

            if (!DatabaseConstants.PAYMENT_METHOD_QRIS.equals(type)) {
                if (name.isEmpty() || number.isEmpty()) {
                    showToast("Harap isi nama dan nomor rekening");
                    return;
                }
                if (number.length() < 6) {
                    showToast("Nomor minimal 6 digit");
                    return;
                }
            } else {
                name = "Default QRIS";
                number = "";
            }

            PaymentMethod method = (existing != null) ? existing : new PaymentMethod();
            method.setUserId(currentUserId);
            method.setType(type);
            method.setProviderName(provider);
            method.setAccountName(name);
            method.setAccountNumber(number);
            method.setDefault(isDef);
            method.setActive(true);

            // Masking
            if (number.length() > 4) {
                method.setMaskedNumber("****" + number.substring(number.length() - 4));
            } else {
                method.setMaskedNumber(number);
            }

            if (existing != null) {
                PaymentMethodRepository.getInstance().updatePaymentMethod(method, new PaymentMethodRepository.SimpleCallback() {
                    @Override public void onSuccess() { if (isDef) PaymentMethodRepository.getInstance().setDefaultMethod(currentUserId, method.getId(), null); dialog.dismiss(); }
                    @Override public void onError(String message) { showToast(message); }
                });
            } else {
                PaymentMethodRepository.getInstance().addPaymentMethod(method, new PaymentMethodRepository.SimpleCallback() {
                    @Override public void onSuccess() { 
                        // If marked as default, we need to unset others. 
                        // Repository setDefaultMethod does this by ID, but we don't have ID yet.
                        // Actually listenToMethods will catch it, but better to be explicit.
                        // For now just dismiss, listen will update.
                        dialog.dismiss(); 
                    }
                    @Override public void onError(String message) { showToast(message); }
                });
            }
        });

        dialog.show();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class SimpleRepoCallback implements PaymentMethodRepository.SimpleCallback {
        @Override public void onSuccess() {}
        @Override public void onError(String message) { showToast(message); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (methodListener != null) methodListener.remove();
    }
}
