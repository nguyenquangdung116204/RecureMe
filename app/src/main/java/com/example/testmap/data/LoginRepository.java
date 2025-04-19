package com.example.testmap.data;

import com.example.testmap.data.model.LoggedInUser;
import com.google.firebase.auth.FirebaseAuth;

public class LoginRepository {

    private static volatile LoginRepository instance;
    private final FirebaseAuth mAuth;

    private LoginRepository() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static LoginRepository getInstance(LoginDataSource loginDataSource) {
        if (instance == null) {
            instance = new LoginRepository();
        }
        return instance;
    }

    public void login(String username, String password, final LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        LoggedInUser user = new LoggedInUser(
                                mAuth.getCurrentUser().getUid(),
                                mAuth.getCurrentUser().getEmail()
                        );
                        callback.onSuccess(new Result.Success<>(user));
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void signUp(String username, String password, final LoginCallback callback) {
        mAuth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        LoggedInUser user = new LoggedInUser(
                                mAuth.getCurrentUser().getUid(),
                                mAuth.getCurrentUser().getEmail()
                        );
                        callback.onSuccess(new Result.Success<>(user));
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}
