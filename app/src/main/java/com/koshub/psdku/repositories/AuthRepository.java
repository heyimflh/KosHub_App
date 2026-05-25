package com.koshub.psdku.repositories;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koshub.psdku.R;
import com.koshub.psdku.services.FirebaseService;
import com.koshub.psdku.utils.DatabaseConstants;
import com.koshub.psdku.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for Authentication.
 * Handles Firebase Auth and Firestore User profile management.
 */
public class AuthRepository {
    private static final String TAG = "KosHubAuth";
    private static AuthRepository instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private AuthRepository() {
        this.auth = FirebaseService.getAuth();
        this.db = FirebaseService.getFirestore();
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public interface UserRoleCallback {
        void onRoleFetched(String role);
        void onError(String message);
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void registerWithEmail(String name, String email, String phone, String password, String role, AuthCallback<FirebaseUser> callback) {
        Log.d(TAG, "REGISTER_START: " + email + " as " + role);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        Log.d(TAG, "REGISTER_SUCCESS: " + firebaseUser.getUid());
                        createUserDocument(firebaseUser.getUid(), name, email, phone, role, DatabaseConstants.PROVIDER_EMAIL, false, new AuthCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "FIRESTORE_USER_CREATE_SUCCESS");
                                firebaseUser.sendEmailVerification()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "EMAIL_VERIFICATION_SENT");
                                            callback.onSuccess(firebaseUser);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "EMAIL_VERIFICATION_FAILED: " + e.getMessage());
                                            callback.onError("Akun berhasil dibuat, tetapi email verifikasi gagal dikirim: " + e.getMessage());
                                        });
                            }

                            @Override
                            public void onError(String message) {
                                callback.onError(message);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "REGISTER_FAILED: " + e.getMessage());
                    callback.onError(mapFirebaseError(e));
                });
    }

    public void loginWithEmail(String email, String password, AuthCallback<FirebaseUser> callback) {
        Log.d(TAG, "LOGIN_EMAIL_START: " + email);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        user.reload().addOnCompleteListener(task -> {
                            if (user.isEmailVerified()) {
                                Log.d(TAG, "LOGIN_SUCCESS_VERIFIED: " + user.getUid());
                                updateEmailVerifiedStatus(user.getUid(), true);
                            } else {
                                Log.d(TAG, "LOGIN_SUCCESS_UNVERIFIED: " + user.getUid());
                            }
                            callback.onSuccess(user);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "LOGIN_FAILED: " + e.getMessage());
                    callback.onError(mapFirebaseError(e));
                });
    }

    public void loginWithGoogle(GoogleSignInAccount account, String selectedRole, AuthCallback<FirebaseUser> callback) {
        Log.d(TAG, "GOOGLE_SIGN_IN_START: " + selectedRole);
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        Log.d(TAG, "GOOGLE_FIREBASE_SUCCESS: " + firebaseUser.getUid());
                        // Check if user document exists
                        db.collection(DatabaseConstants.COLLECTION_USERS).document(firebaseUser.getUid()).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String role = documentSnapshot.getString(DatabaseConstants.FIELD_ROLE);
                                        Log.d(TAG, "GOOGLE_USER_DOC_EXISTS: " + role);
                                        callback.onSuccess(firebaseUser);
                                    } else {
                                        // Create new user document
                                        Log.d(TAG, "GOOGLE_CREATE_USER_DOC: " + selectedRole);
                                        createUserDocument(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail(), "", selectedRole, DatabaseConstants.PROVIDER_GOOGLE, true, new AuthCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                callback.onSuccess(firebaseUser);
                                            }

                                            @Override
                                            public void onError(String message) {
                                                callback.onError(message);
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> callback.onError(mapFirebaseError(e)));
                    }
                })
                .addOnFailureListener(e -> callback.onError(mapFirebaseError(e)));
    }

    private void createUserDocument(String uid, String name, String email, String phone, String role, String provider, boolean emailVerified, AuthCallback<Void> callback) {
        Map<String, Object> user = new HashMap<>();
        user.put(DatabaseConstants.FIELD_ID, uid);
        user.put(DatabaseConstants.FIELD_NAME, name);
        user.put(DatabaseConstants.FIELD_EMAIL, email);
        user.put(DatabaseConstants.FIELD_PHONE, phone);
        user.put(DatabaseConstants.FIELD_ROLE, role);
        user.put(DatabaseConstants.FIELD_PROVIDER, provider);
        user.put(DatabaseConstants.FIELD_EMAIL_VERIFIED, emailVerified);
        user.put(DatabaseConstants.FIELD_CREATED_AT, System.currentTimeMillis());
        user.put(DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());
        user.put(DatabaseConstants.FIELD_PROFILE_IMAGE_URL, "");

        db.collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(mapFirebaseError(e)));
    }

    public void getUserRole(String uid, UserRoleCallback callback) {
        db.collection(DatabaseConstants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString(DatabaseConstants.FIELD_ROLE);
                        callback.onRoleFetched(role);
                    } else {
                        callback.onError("User data not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(mapFirebaseError(e)));
    }

    public void sendPasswordReset(String email, AuthCallback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(mapFirebaseError(e)));
    }

    public void resendEmailVerification(AuthCallback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError(mapFirebaseError(e)));
        } else {
            callback.onError("User not logged in");
        }
    }

    private void updateEmailVerifiedStatus(String uid, boolean verified) {
        db.collection(DatabaseConstants.COLLECTION_USERS).document(uid)
                .update(DatabaseConstants.FIELD_EMAIL_VERIFIED, verified,
                        DatabaseConstants.FIELD_UPDATED_AT, System.currentTimeMillis());
    }

    public void logout(Activity activity, AuthCallback<Void> callback) {
        Log.d(TAG, "LOGOUT_START");
        auth.signOut();
        
        // Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d(TAG, "GOOGLE_SIGN_OUT_COMPLETE");
            new SessionManager(activity).logoutUser();
            if (callback != null) callback.onSuccess(null);
        });
    }

    private String mapFirebaseError(Exception e) {
        String message = e.getMessage();
        if (message == null) return "An unknown error occurred";
        
        if (message.contains("email address is already in use")) {
            return "Email sudah terdaftar. Silakan gunakan email lain atau login.";
        } else if (message.contains("password is invalid") || message.contains("weak-password")) {
            return "Kata sandi terlalu lemah. Minimal 6 karakter.";
        } else if (message.contains("user-not-found") || message.contains("wrong-password") || message.contains("invalid-credential")) {
            return "Email atau kata sandi salah.";
        } else if (message.contains("network-request-failed")) {
            return "Gagal terhubung ke internet. Periksa koneksi Anda.";
        } else if (message.contains("too-many-requests")) {
            return "Terlalu banyak percobaan. Silakan coba lagi nanti.";
        }
        
        return message;
    }
}
