package com.blazingapps.asus.medsys;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "TAGZ";
    private static final String MYPREF = "mypref";
    private static final String REGISTERED = "registered";
    EditText phoneNumberEt;
    EditText uidEt;
    FirebaseFirestore db;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        sharedPreferences =getSharedPreferences(MYPREF,MODE_PRIVATE);
        if (sharedPreferences.getBoolean(REGISTERED,false)){
            startActivity(new Intent(this,MainActivity.class));
        }
        mAuth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_login);
        progressBar = findViewById(R.id.progress_circular);
        db = FirebaseFirestore.getInstance();
        phoneNumberEt = findViewById(R.id.phone_no);
        uidEt = findViewById(R.id.uid);
    }


    public void phoneVerify(View view) {
        view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91"+phoneNumberEt.getText().toString(),        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    public void registerToFirebase(String name, String address, String aadhar, String gender, double dob_timestamp, GeoPoint geoPoint, String height, String weight){
        Map<String, Object> user = new HashMap<>();
        user.put("aadhar", aadhar);
        user.put("dob", new Date((long) dob_timestamp));
        user.put("location", geoPoint);
        user.put("lost",false);
        user.put("name",name);
        user.put("sex",gender);
        user.put("spam",false);
        user.put("uid",uidEt.getText().toString());

        db.collection("patients").document(mAuth.getCurrentUser().getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        startActivity(new Intent(LoginActivity.this,MainActivity.class));
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(REGISTERED,true);
                        editor.commit();
                        progressBar.setVisibility(View.GONE);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }
    public void showCompleteProfile(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Details");
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.layout_complete_profile, null);
        final EditText patient_name = alertLayout.findViewById(R.id.et_name);
        final EditText patient_address = alertLayout.findViewById(R.id.et_address);
        final EditText patient_aadhar = alertLayout.findViewById(R.id.et_aadhar);
        final RadioButton male = alertLayout.findViewById(R.id.male);
        final DatePicker dob = alertLayout.findViewById(R.id.date_picker);

        builder.setCancelable(false);
        builder.setView(alertLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = patient_name.getText().toString();
                String address = patient_address.getText().toString();
                String aadhar = patient_aadhar.getText().toString();
                String gender = "M";
                String str_date=dob.getDayOfMonth()+"-"+dob.getMonth()+"-"+dob.getYear();
                DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                Date date = null;
                try {
                    date = (Date)formatter.parse(str_date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                System.out.println("Today is " +date.getTime());
                if (!male.isChecked()){
                    gender = "F";
                }
                double dob_timestamp = date.getTime();
                GeoPoint geoPoint = new GeoPoint(10,10);
                String height = null;
                String weight = null;

                registerToFirebase(
                        name,
                        address,
                        aadhar,
                        gender,
                        dob_timestamp,
                        geoPoint,
                        height,
                        weight);
            }
        });
        builder.show();
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onVerificationCompleted(PhoneAuthCredential credential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:" + credential);

            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e);

            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                // ...
            } else if (e instanceof FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                // ...
            }

            // Show a message and update the UI
            // ...
        }

        @Override
        public void onCodeSent(String verificationId,
                PhoneAuthProvider.ForceResendingToken token) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:" + verificationId);

            // Save verification ID and resending token so we can use them later
//            mVerificationId = verificationId;
//            mResendToken = token;

            // ...
        }
    };

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            showCompleteProfile();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }
}
