package com.example.testmap.data;

import com.example.testmap.data.model.LoggedInUser;
import com.example.testmap.data.Result;

public interface LoginCallback {
    void onSuccess(Result<LoggedInUser> result);
    void onFailure(Exception e);
}
