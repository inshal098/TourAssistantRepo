package com.tourassistant.coderoids.home.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.tourassistant.coderoids.R;
import com.tourassistant.coderoids.adapters.PersonalPicturesUploads;
import com.tourassistant.coderoids.adapters.PortfolioAdapter;
import com.tourassistant.coderoids.adapters.UserReviewsAdapter;
import com.tourassistant.coderoids.chatmodule.ChatParentActivity;
import com.tourassistant.coderoids.helpers.AppHelper;
import com.tourassistant.coderoids.models.NewsFeedModel;
import com.tourassistant.coderoids.models.Profile;
import com.tourassistant.coderoids.starttrip.ReportHazard;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    TextView tvEditProfile;
    TextView tvFollowingCount, tvFollowersCount, tvPostCount, tvName, tvWebsite, tvDescription, tvTrackingState, tvIntrest, tvPersonalPictures, tvTripsPicture;
    ImageButton ibAddPreferences;
    SwitchMaterial smTrackState;
    CircleImageView circleImageView;
    ProgressDialog dialog;
    GridView intests;
    ActionBar actionBar;
    private boolean rowState[] = new boolean[0];
    ExtendedFloatingActionButton actionButton;
    FloatingActionButton floatingChatIc;
    SharedPreferences.Editor editorLogin;
    SharedPreferences prefLogin;
    RecyclerView rvNews, rvTripPhoto , rvReviews;
    LinearLayoutManager llmNews, llTripPhoto ,llReviews;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.profile_view, container, false);
        intializeView(v);
        return v;
    }

    private void intializeView(View v) {
        prefLogin = getActivity().getSharedPreferences("logindata", Context.MODE_PRIVATE);
        editorLogin = prefLogin.edit();
        editorLogin.apply();
        tvName = v.findViewById(R.id.user_name);
        circleImageView = v.findViewById(R.id.profile_photo);
        rvReviews = v.findViewById(R.id.user_reviews);
        tvPersonalPictures = v.findViewById(R.id.pt_);
        tvTripsPicture = v.findViewById(R.id.pt_trip);
        rvNews = v.findViewById(R.id.rv_news_feed);
        rvTripPhoto = v.findViewById(R.id.rv_trip_photos);
        llmNews = new LinearLayoutManager(getContext());
        llmNews.setOrientation(LinearLayoutManager.VERTICAL);

        llReviews = new LinearLayoutManager(getContext());
        llReviews.setOrientation(LinearLayoutManager.HORIZONTAL);

        llTripPhoto = new LinearLayoutManager(getContext());
        llTripPhoto.setOrientation(LinearLayoutManager.VERTICAL);

        floatingChatIc = v.findViewById(R.id._chat);
        tvEditProfile = v.findViewById(R.id.textEditProfile);
        tvFollowersCount = v.findViewById(R.id.tv_followers_count);
        tvFollowingCount = v.findViewById(R.id.tv_following_count);
        tvPostCount = v.findViewById(R.id.tv_post_count);
        tvWebsite = v.findViewById(R.id.website);
        tvDescription = v.findViewById(R.id.description);
        ibAddPreferences = v.findViewById(R.id.addIntrest);
        smTrackState = v.findViewById(R.id.sw_tracking_state);
        tvTrackingState = v.findViewById(R.id.tracking_state_tv);
        actionButton = v.findViewById(R.id.create_a_post);
        tvIntrest = v.findViewById(R.id.prefs);
        dialog = new ProgressDialog(getActivity());
        actionBar = ((AppCompatActivity)
                requireActivity()).getSupportActionBar();

        tvEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment);
            }
        });
        if (AppHelper.currentProfileInstance != null) {

            if (prefLogin.getString("userTracking", "0").matches("1")) {
                tvTrackingState.setText("Location is Being Tracked");
                tvTrackingState.setBackgroundColor(getActivity().getResources().getColor(R.color.green));
                smTrackState.setChecked(true);
            } else {
                smTrackState.setChecked(false);
                tvTrackingState.setText("Location Tracking is Off");
                tvTrackingState.setBackgroundColor(getActivity().getResources().getColor(R.color.red));
            }
        }


        smTrackState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
                DocumentReference uidRef = rootRef.collection("Users").document(AppHelper.currentProfileInstance.getUserId());
                if (smTrackState.isChecked()) {
                    editorLogin.putString("userTracking", "1").apply();
                    uidRef.update("userTracking", "1");
                    tvTrackingState.setText("Enabled , Your Location is Being Tracked");
                    tvTrackingState.setBackgroundColor(getActivity().getResources().getColor(R.color.green));
                } else {
                    uidRef.update("userTracking", "0");
                    editorLogin.putString("userTracking", "0").apply();
                    tvTrackingState.setText("Disabled , Your Location Tracking is Off");
                    tvTrackingState.setBackgroundColor(getActivity().getResources().getColor(R.color.red));
                }
            }
        });
        try {
            if (AppHelper.currentProfileInstance != null) {
                Profile profileList = AppHelper.currentProfileInstance;
                if (profileList.getUserName() != null)
                    actionBar.setTitle(profileList.getUserName());
                tvName.setText(profileList.getUserName());
                tvPostCount.setText(profileList.getTotalPosts());
                tvFollowersCount.setText(profileList.getFollowers());
                tvFollowingCount.setText(profileList.getFollowing());
                tvWebsite.setText(profileList.getWebsite());
                tvDescription.setText(profileList.getAboutDescription());
                tvIntrest.setText(AppHelper.getUserIntrests(AppHelper.interestUser));
                if (profileList.getProfileImage() != null && !profileList.getProfileImage().toString().matches("")) {
                    byte[] bytes = profileList.getProfileImage().toBytes();
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    circleImageView.setImageBitmap(bmp);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(ex);
        }


        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ReportHazard.class));
            }
        });
        ibAddPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        floatingChatIc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ChatParentActivity.class).putExtra("type", "Profile"));
            }
        });
        fetchUserPosts();

        tvPersonalPictures.setBackgroundColor(getActivity().getResources().getColor(R.color.appTheme2));
        tvPersonalPictures.setTextColor(getActivity().getResources().getColor(R.color.white));
        rvNews.setVisibility(View.VISIBLE);
        rvTripPhoto.setVisibility(View.GONE);
        tvPersonalPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvPersonalPictures.setBackgroundColor(getActivity().getResources().getColor(R.color.appTheme2));
                tvPersonalPictures.setTextColor(getActivity().getResources().getColor(R.color.white));

                tvTripsPicture.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                tvTripsPicture.setTextColor(getActivity().getResources().getColor(R.color.black));
                rvNews.setVisibility(View.VISIBLE);
                rvTripPhoto.setVisibility(View.GONE);
            }
        });

        tvTripsPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvTripsPicture.setBackgroundColor(getActivity().getResources().getColor(R.color.appTheme2));
                tvTripsPicture.setTextColor(getActivity().getResources().getColor(R.color.white));
                tvPersonalPictures.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                tvPersonalPictures.setTextColor(getActivity().getResources().getColor(R.color.black));
                rvNews.setVisibility(View.GONE);
                rvTripPhoto.setVisibility(View.VISIBLE);
            }
        });
        tvPostCount.setText("0");
    }

    private void fetchUserPosts() {
        if (AppHelper.currentProfileInstance != null && AppHelper.currentProfileInstance.getUserId() != null) {
            FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
            rootRef.collection("Users").document(AppHelper.currentProfileInstance.getUserId()).collection("NewsFeed").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isComplete()) {
                        List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                        if (documentSnapshots != null) {
                            tvPostCount.setText(documentSnapshots.size()+"");
                            PersonalPicturesUploads newsListingAdapter = new PersonalPicturesUploads(getContext(), documentSnapshots);
                            rvNews.setAdapter(newsListingAdapter);
                            rvNews.setLayoutManager(new GridLayoutManager(getContext(), 3));
                        }
                    }
                }
            });

            rootRef.collection("Trips").document(AppHelper.currentProfileInstance.getUserId())
                    .collection("UserTrips").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isComplete()) {
                        List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                        if (documentSnapshots != null) {
                            PortfolioAdapter adapter = new PortfolioAdapter(getContext(), documentSnapshots);
                            rvTripPhoto.setAdapter(adapter);
                            rvTripPhoto.setLayoutManager(new GridLayoutManager(getContext(), 3));
//                            documentSnapshots.size();
//
                        }
                    }
                }
            });

            rootRef.collection("Users").document(AppHelper.currentProfileInstance.getUserId()).collection("reviews").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isComplete()) {
                        List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                        if (documentSnapshots != null) {
                            documentSnapshots.size();
                            UserReviewsAdapter newsListingAdapter = new UserReviewsAdapter(getContext(), documentSnapshots);
                            rvReviews.setAdapter(newsListingAdapter);
                            rvReviews.setLayoutManager(llReviews);
                        }
                    }
                }
            });
        }
    }
}