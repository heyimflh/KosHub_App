package com.koshub.psdku.repositories;

import com.koshub.psdku.models.User;

/**
 * Repository for Authentication.
 * Placeholder for Firebase Auth integration.
 */
public class AuthRepository {
    private static AuthRepository instance;
    private User currentUser;

    private AuthRepository() {
        // Initially no user is logged in
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

    /**
     * Get dummy current user.
     * TODO: Connect to Firebase Auth.
     */
    public User getCurrentUserDummy() {
        if (currentUser == null) {
            currentUser = new User("dummy_id", "Pengguna KosHub", "user@example.com", "Mahasiswa");
        }
        return currentUser;
    }

    public void login(String email, String password, AuthCallback<User> callback) {
        // TODO: Implement Firebase Login
        if (callback != null) {
            currentUser = new User("dummy_id", "Logged In User", email, "Mahasiswa");
            callback.onSuccess(currentUser);
        }
    }

    public void logout() {
        // TODO: Implement Firebase Logout
        currentUser = null;
    }
}
