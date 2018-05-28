package com.example.android.tasku.JavaUtils;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by john.osorio on 18/08/2017.
 */

public class UserUtils {

    public String emailAddress;
    public String firstName;
    public String lastName;
    private String userMobile;
    public String userID;
    public String firebaseToken;

    public UserUtils(){}

    public UserUtils(String emailAddress, String firstName, String lastName){
        this.emailAddress = emailAddress;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UserUtils(String emailAddress, String firstName, String lastName, String userMobile, String userID, String firebaseToken) {

        this.emailAddress = emailAddress;
        this.userMobile = userMobile;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userID = userID;
        this.firebaseToken = firebaseToken;
    }

    public String getEmailAddress(){return emailAddress; }

    public String getUserMobile() {return userMobile; }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getUserID() { return userID; }

    public String getFirebaseToken() { return firebaseToken; }
}
