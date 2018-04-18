package com.example.firenova.photo1;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.firenova.photo1.data.PetContract.PetEntry;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;


import static android.media.MediaRecorder.VideoSource.CAMERA;
import static com.example.firenova.photo1.data.PetContract.PetEntry._ID;


public class Photo extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the pet data loader
     */
    private static final int EXISTING_PET_LOADER = 0;

    /**
     * Content URI for the existing pet (null if it's a new pet)
     */
    private Uri mCurrentPetUri;

    /**
     * EditText field to enter the pet's name
     */
    private TextView mNameEditText;

    /**
     * EditText field to enter the pet's breed
     */
    private TextView reallocation;
    private TextView reallocation2;

    /**
     * EditText field to enter the pet's weight
     */
    private TextView time;

    /**
     * EditText field to enter the pet's gender
     */
    //private ImageView mGenderSpinner;
    private String photo1;

    /**
     * Boolean flag that keeps track of whether the pet has been edited (true) or not (false)
     */
    private boolean mPetHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mPetHasChanged boolean to true.
     */
//    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            mPetHasChanged = true;
//            return false;
//        }
//    };

    final private int SELECT_FILE = 71;
    private ImageView ivImage;
    private DisplayMetrics mPhone;

    Calendar c = new GregorianCalendar();
    final int year = c.get(Calendar.YEAR);
    String yearString = Integer.toString(year);
    int month = c.get(Calendar.MONTH);
    int realmonth = month + 1;
    int day = c.get(Calendar.DAY_OF_MONTH);
    int hour = c.get(Calendar.HOUR_OF_DAY);
    int minute = c.get(Calendar.MINUTE);
    int second = c.get(Calendar.SECOND);
    String realrandom = UUID.randomUUID().toString();
    String random = realrandom.substring(0, 8);
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    String moreinfo = "";

    boolean photodata = true;
    byte[] data1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        FirebaseStorage.getInstance().getReference();

        LocationManager locationManager;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//                String provider = LocationManager.GPS_PROVIDER;
//                String provider = "gps";

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new pet or editing an existing one.
        Intent intent = getIntent();
        mCurrentPetUri = intent.getData();


        // Find all relevant views that we will need to read user input from
        ivImage = (ImageView) findViewById(R.id.img);
        mNameEditText = (TextView) findViewById(R.id.edit_pet_name);
        reallocation = (TextView) findViewById(R.id.reallocation);
        reallocation2 = (TextView) findViewById(R.id.reallocation2);
        time = (TextView) findViewById(R.id.time);


        // If the intent DOES NOT contain a pet content URI, then we know that we are
        // creating a new pet.
        if (mCurrentPetUri == null) {
            // This is a new pet, so change the app bar to say "Add a Pet"
            setTitle(getString(R.string.editor_activity_title_new_pet));

            Location location = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(location);
            locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);

            selectImage();

            mNameEditText.setText("白蟻名字");

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            //invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing pet, so change app bar to say "View Pet"
            setTitle(getString(R.string.editor_activity_title_view_pet));

            // Initialize a loader to read the pet data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);

            mNameEditText.setText("鑑定中");
            // Read from the fiirebase database
            String[] column = {PetEntry.COLUMN_PET_NAME};
            String[] column2 = {PetEntry.TIME};
            Cursor cursor = getContentResolver().query(mCurrentPetUri, column, null, null, null);
            Cursor cursor2 = getContentResolver().query(mCurrentPetUri, column2, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    if (cursor2.moveToFirst()) {


                        do {

                            int index = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
                            int index2 = cursor2.getColumnIndex(PetEntry.TIME);

                            String a123 = cursor.getString(index);
                            String lasttime = cursor2.getString(index2);
                            String[] parts = lasttime.split(" ");
                            String[] pair2 = parts[1].split(":");
                            Toast.makeText(Photo.this, "更改成" + a123, Toast.LENGTH_LONG).show();

                            DatabaseReference myRef2 = database.getReference().child(parts[0] + "/" + pair2[0].trim() + "/" + pair2[1].trim() + "/" + pair2[2].trim() + "/" + a123 + "/" + "name");
                            myRef2.addValueEventListener(new ValueEventListener() {

                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    String value = dataSnapshot.getValue(String.class);
                                    //  Toast.makeText(Photo.this, "更改成" + value, Toast.LENGTH_LONG).show();
                                    if (value != null) {

                                        mNameEditText.setText(value);
                                        ContentValues values = new ContentValues();
                                        String value2 = mNameEditText.getText().toString().trim();
                                        values.put(PetEntry.COLUMN_PET_NAME, value2);

                                        getContentResolver().update(mCurrentPetUri, values, null, null);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError firebaseError) {
                                    Log.d("FireBaseTraining", "The read failed: " + firebaseError.getMessage());
                                }
                            });

                        } while (cursor.moveToNext());
                    }
                }
            } catch (Exception e) {
                Toast.makeText(Photo.this, "未鑑定完成", Toast.LENGTH_LONG).show();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                    cursor2.close();
                }
            }
            //  mNameEditText.setEnabled(false);
        }
        updateWithTime();
        Button btnSelect;
        btnSelect = (Button) findViewById(R.id.camera);
        btnSelect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void selectImage() {
        final CharSequence[] items = {"拍照", "從相簿選擇",
                "取消"};

        AlertDialog.Builder builder = new AlertDialog.Builder(Photo.this);
        builder.setTitle("新增照片!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(Photo.this);

                if (items[item].equals("拍照")) {
                    // userChoosenTask = "Take Photo";
                    if (result)
                        cameraIntent();

                } else if (items[item].equals("從相簿選擇")) {
                    // userChoosenTask = "Choose from Library";
                    if (result)
                        galleryIntent();

                } else if (items[item].equals("取消")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String mImagePath;
        mImagePath = Environment.getExternalStorageDirectory() + timeStamp + ".jpg";
        final File tmpCameraFile = new File(mImagePath);
        startActivityForResult(new Intent(Intent.ACTION_PICK).setType("image/*").putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpCameraFile)), SELECT_FILE);

        //   tmpCameraFile.delete();


//        //讀取手機解析度
//        mPhone = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(mPhone);
//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);//
//        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    private void cameraIntent() {
        //獲取系統版本
        final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        //讀取手機解析度
        mPhone = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mPhone);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File tmpFile = new File(Environment.getExternalStorageDirectory(), "image.jpg");

        if (currentapiVersion < 24) {

            Uri outputFileUri = Uri.fromFile(tmpFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        } else {

            //兼容android7.0 使用共享文件的形式
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, tmpFile.getAbsolutePath());
            Uri uri = this.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        startActivityForResult(intent, CAMERA);

        final boolean bool = tmpFile.delete();
        String a = "拍完照請稍候片刻 " + bool;
        Toast.makeText(Photo.this, a, Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_FILE) {
                moreinfo = "  ( 此資料從相簿上傳 )  ";
//                onSelectFromGalleryResult(data);

                Uri uri = data.getData();
                regeoLatlng(uriToRealPath(uri));
                getPhotoLocation(uriToRealPath(uri));

            } else if (requestCode == CAMERA) {

                Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/image.jpg");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                data1 = bytes.toByteArray();

                if (bitmap.getWidth() > bitmap.getHeight()) ScalePic(bitmap,
                        mPhone.heightPixels);
                else ScalePic(bitmap, mPhone.widthPixels);
            }
        }
    }

    private String uriToRealPath(Uri uri) {
        Cursor cursor = managedQuery(uri,
                new String[]{MediaStore.Images.Media.DATA},
                null,
                null,
                null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        return path;
    }

    private void regeoLatlng(String path) {
        // 结果UI展示
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bm = BitmapFactory.decodeFile(path, options);


        //  savePNG_After(bm, path);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        data1 = bytes.toByteArray();
        byte[] appicon = bytes.toByteArray();
        photo1 = Base64.encodeToString(appicon, Base64.DEFAULT);
        ivImage.setImageBitmap(bm);

    }

//    public static void savePNG_After(Bitmap bitmap, String name) {
//        File file = new File(name);
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
//                out.flush();
//                out.close();
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }

    public void getPhotoLocation(String imagePath) {
        Log.i("TAG", "getPhotoLocation==" + imagePath);
        float output1;
        float output2;

        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            String datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);// 拍摄时间
            String latValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String lngValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String lngRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);


            if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
                output1 = convertRationalLatLonToFloat(latValue, latRef);
                output2 = convertRationalLatLonToFloat(lngValue, lngRef);

                String lo1 = "真緯度 : " + output1;
                String lo2 = "真經度 : " + output2;

                reallocation.setText(lo1);
                reallocation2.setText(lo2);
                time.setText(convertRationaltime(datetime));
                photodata = false;
            } else {
                String lo1 = "相片沒紀錄緯度 ";
                String lo2 = "相片沒紀錄經度";
                reallocation.setText(lo1);
                reallocation2.setText(lo2);
                time.setText(convertRationaltime(datetime));
            }


//            setDiffColor(phoneTV, "手机型号：" + deviceName + "," + deviceModel);
//            setDiffColor(latlngTV, "经纬度：" + output1 + ";" + output2);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float convertRationalLatLonToFloat(String rationalString, String ref) {

        String[] parts = rationalString.split(",");

        String[] pair;
        pair = parts[0].split("/");
        double degrees = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        pair = parts[1].split("/");
        double minutes = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        pair = parts[2].split("/");
        double seconds = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
        if ((ref.equals("S") || ref.equals("W"))) {
            return (float) -result;
        }
        return (float) result;
    }

    private static String convertRationaltime(String rationalString) {


        String[] parts = rationalString.split(" ");

        String[] pair1;
        pair1 = parts[0].split(":");

        String[] pair2;
        pair2 = parts[1].split(":");

        return pair1[0] + a1 + pair1[1] + a1 + pair1[2].trim() + a2 + pair2[0].trim() + a3 + pair2[1] + a3 + pair2[2].trim();

    }


    private void ScalePic(Bitmap bitmap, int phone) {

        //如果圖片寬度大於手機寬度則進行縮放，否則直接將圖片放入ImageView內
        if (bitmap.getWidth() > phone) {
            //判斷縮放比例
            final float mScale = (float) phone / (float) bitmap.getWidth();

            Matrix mMat = new Matrix();
            mMat.setScale(mScale * 0.25f, mScale * 0.25f);

            Bitmap mScaleBitmap = Bitmap.createBitmap(bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    mMat,
                    false);
            photo1 = convertIconToString(mScaleBitmap);
            ivImage.setImageBitmap(mScaleBitmap);

        } else {
            photo1 = convertIconToString(bitmap);
            ivImage.setImageBitmap(bitmap);
        }
    }


//    @SuppressWarnings("deprecation")
//    private void onSelectFromGalleryResult(Intent data) {
//
//        Bitmap bm = null;
//        if (data != null) {
//            try {
//                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//        bm.compress(Bitmap.CompressFormat.PNG, 100, bytes);
//        data1 = bytes.toByteArray();
//
//        if (bm.getWidth() > bm.getHeight()) ScalePic(bm,
//                mPhone.heightPixels);
//        else ScalePic(bm, mPhone.widthPixels);
//
////        photo1 = convertIconToString(bm);
////        bm = convertStringToIcon(photo1);
////        ivImage.setImageBitmap(bm);
//
//    }

    public static String convertIconToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();// outputstream
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] appicon = baos.toByteArray();// 转为byte数组
        return Base64.encodeToString(appicon, Base64.DEFAULT);

    }

    public static Bitmap convertStringToIcon(String st) {
        // OutputStream out;
        Bitmap bitmap;
        try {
            // out = new FileOutputStream("/sdcard/aa.jpg");
            byte[] bitmapArray;
            bitmapArray = Base64.decode(st, Base64.DEFAULT);
            bitmap =
                    BitmapFactory.decodeByteArray(bitmapArray, 0,
                            bitmapArray.length);
            // bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
        }

        public void onProviderDisabled(String provider) {
            updateWithNewLocation(null);
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private void updateWithNewLocation(Location location) {
        String latLongString, latLongString2;

        TextView myLocationText;
        TextView myLocationText2;
        myLocationText = (TextView) findViewById(R.id.reallocation);
        myLocationText2 = (TextView) findViewById(R.id.reallocation2);
        try {
            Thread.sleep(0);//因为真机获取gps数据需要一定的时间，为了保证获取到，采取系统休眠的延迟方法
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            // Geocoder geocoder = new Geocoder(this);
            Geocoder geocoder = new Geocoder(this, Locale.CHINA);
            List places;

            try {
//                Thread.sleep(2000);
                places = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 4);
////                                Thread.sleep(2000);
                Toast.makeText(Photo.this, places.size() + "", Toast.LENGTH_LONG).show();
//                System.out.println(places.size() + "");
            } catch (Exception e) {
                e.printStackTrace();
            }

//            String placename = "";
            //           if (places != null && places.size() > 0) {
//                // placename=((Address)places.get(0)).getLocality();
//                //一下的信息将会具体到某条街
//                //其中getAddressLine(0)表示国家，getAddressLine(1)表示精确到某个区，getAddressLine(2)表示精确到具体的街
//                placename = ((Address) places.get(0)).getAddressLine(0) + ", " + System.getProperty("line.separator")
//                        + ((Address) places.get(0)).getAddressLine(1) + ", "
//                        + ((Address) places.get(0)).getAddressLine(2);
//            }

            latLongString = "緯度: " + lat;
            latLongString2 = " 經度: " + lng;
//            Toast.makeText(Photo.this, placename, Toast.LENGTH_LONG).show();
        } else {
            latLongString = "無法獲取地理信息";
            latLongString2 = "無法獲取地理信息";
        }
        myLocationText.setText(latLongString);
        myLocationText2.setText(latLongString2);

    }

    /**
     * Get user input from editor and save pet into database.
     */


    public static final String a1 = "/";
    public static final String a2 = " ";
    public static final String a3 = ":";

    private void updateWithTime() {

        //獲取當前時間
        Calendar c = new GregorianCalendar();
        final int year = c.get(Calendar.YEAR);
        String yearString = Integer.toString(year);
        final int month = c.get(Calendar.MONTH);
        final int realmonth = month + 1;
        String monthString = Integer.toString(realmonth);
        final int day = c.get(Calendar.DAY_OF_MONTH);
        String dayString = Integer.toString(day);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        String hourString = Integer.toString(hour);
        final int minute = c.get(Calendar.MINUTE);
        String minuteString = Integer.toString(minute);
        final int second = c.get(Calendar.SECOND);
        String secondString = Integer.toString(second);

        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String timeString = yearString + a1 + monthString + a1 + dayString + a2 + hourString + a3 + minuteString + a3 + secondString;

        TextView time;
        time = (TextView) findViewById(R.id.time);
        time.setText(timeString);

        // return yearString + monthString + dayString + hourString;
    }

    static final String setphoto = "請提供照片";

    private void savePet() {
        if (data1 == null) {
            Toast.makeText(Photo.this, setphoto, Toast.LENGTH_LONG).show();
        } else {
            String monthString = Integer.toString(realmonth);
            String dayString = Integer.toString(day);
            String hourString = Integer.toString(hour);
            String minuteString = Integer.toString(minute);
            String secondString = Integer.toString(second);
            if (month < 10) {
                monthString = "0" + monthString;
            }
            if (day < 10) {
                dayString = "0" + dayString;
            }
            if (hour < 10) {
                hourString = "0" + hourString;
            }
            if (minute < 10) {
                minuteString = "0" + minuteString;
            }
            if (second < 10) {
                secondString = "0" + secondString;
            }

            String b = yearString + "-" + monthString + "-" + dayString + "-" + hourString + "-" + minuteString + "-" + secondString + "-";

            String path = "firememes/" + b + realrandom + ".png";
            StorageReference firememeRef = storage.getReference(path);
            String a = mNameEditText.getText().toString().trim() + moreinfo + " 時間 :" + time.getText().toString().trim() + " " + reallocation.getText().toString().trim() + " " + reallocation2.getText().toString().trim();

            StorageMetadata metadata = new StorageMetadata.Builder().setCustomMetadata("text", a).build();
            firememeRef.putBytes(data1, metadata);

            String timeString = time.getText().toString();
            String[] parts = timeString.split(" ");
            String[] pair2 = parts[1].split(":");

            DatabaseReference myRef2 = database.getReference().child(parts[0] + "/" + pair2[0].trim() + "/" + pair2[1].trim() + "/" + pair2[2].trim() + "/" + random);
            myRef2.setValue(realrandom);
            DatabaseReference myRef = database.getReference().child(parts[0] + "/" + pair2[0].trim() + "/" + pair2[1].trim() + "/" + pair2[2].trim() + "/" + random + "/" + "name");
            myRef.setValue("鑑定中");


            String nameString = mNameEditText.getText().toString().trim();
            String locationString = reallocation.getText().toString().trim();
            String locationString2 = reallocation2.getText().toString().trim();


            // Check if this is supposed to be a new pet
            // and check if all the fields in the editor are blank
            if (mCurrentPetUri == null &&
                    TextUtils.isEmpty(nameString) && TextUtils.isEmpty(locationString) &&
                    TextUtils.isEmpty(timeString) && TextUtils.isEmpty(photo1)) {
                // Since no fields were modified, we can return early without creating a new pet.
                // No need to create ContentValues and no need to do any ContentProvider operations.
                return;
            }
            // Create a ContentValues object where column names are the keys,
            // and pet attributes from the editor are the values.
            ContentValues values = new ContentValues();
            values.put(PetEntry.COLUMN_PET_NAME, random);
            values.put(PetEntry.LOCATION, locationString);
            values.put(PetEntry.LOCATION2, locationString2);
            values.put(PetEntry.TIME, timeString);
            values.put(PetEntry.PHOTO, photo1);


            // Determine if this is a new or existing pet by checking if mCurrentPetUri is null or not
            if (mCurrentPetUri == null) {
                // This is a NEW pet, so insert a new pet into the provider,
                // returning the content URI for the new pet.
                Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, values);

                // Show a toast message depending on whether or not the insertion was successful.
                if (newUri == null) {
                    // If the new content URI is null, then there was an error with insertion.
                    Toast.makeText(this, getString(R.string.editor_insert_pet_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the insertion was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_insert_pet_successful),
                            Toast.LENGTH_SHORT).show();

                    finish();

                }
            } else {
                // Otherwise this is an EXISTING pet, so update the pet with content URI: mCurrentPetUri
                // and pass in the new ContentValues. Pass in null for the selection and selection args
                // because mCurrentPetUri will already identify the correct row in the database that
                // we want to modify.
                int rowsAffected = getContentResolver().update(mCurrentPetUri, values, null, null);

                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, getString(R.string.editor_update_pet_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_update_pet_successful),
                            Toast.LENGTH_SHORT).show();
                }


            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
//        if (mCurrentPetUri == null) {
//            MenuItem menuItem = menu.findItem(R.id.action_delete);
//            menuItem.setVisible(false);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                if (mCurrentPetUri == null) {
                    savePet();
                }


                return true;
            // Respond to a click on the "Delete" menu option
//            case R.id.action_delete:
//                // Pop up confirmation dialog for deletion
//                //showDeleteConfirmationDialog();
//                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(Photo.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(Photo.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all pet attributes, define a projection that contains
        // all columns from the pet table
        String[] projection = {
                _ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.LOCATION,
                PetEntry.LOCATION2,
                PetEntry.TIME,
                PetEntry.PHOTO
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentPetUri,         // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
            int location1ColumnIndex = cursor.getColumnIndex(PetEntry.LOCATION);
            int location2ColumnIndex = cursor.getColumnIndex(PetEntry.LOCATION2);
            int timeColumnIndex = cursor.getColumnIndex(PetEntry.TIME);
            int photoColumnIndex = cursor.getColumnIndex(PetEntry.PHOTO);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String location1 = cursor.getString(location1ColumnIndex);
            String location2 = cursor.getString(location2ColumnIndex);
            String photo = cursor.getString(photoColumnIndex);
            String time = cursor.getString(timeColumnIndex);

            // Update the views on the screen with the values from the database
            //mNameEditText.setText(name);
            Bitmap a;
            a = convertStringToIcon(photo);
            ivImage.setImageBitmap(a);
            mNameEditText.setText(name);
            reallocation.setText(location1);
            reallocation2.setText(location2);
            this.time.setText(time);

            // Gender is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is Male, 2 is Female).
            // Then call setSelection() so that option is displayed on screen as the current selection.

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        reallocation.setText("");
        time.setText("");
        // Select "Unknown" gender
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


//    /**
//     * Prompt the user to confirm that they want to delete this pet.
//     */
//    private void showDeleteConfirmationDialog() {
//        // Create an AlertDialog.Builder and set the message, and click listeners
//        // for the postivie and negative buttons on the dialog.
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage(R.string.delete_dialog_msg);
//        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // User clicked the "Delete" button, so delete the pet.
//                deletePet();
//            }
//        });
//        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // User clicked the "Cancel" button, so dismiss the dialog
//                // and continue editing the pet.
//                if (dialog != null) {
//                    dialog.dismiss();
//                }
//            }
//        });
//
//        // Create and show the AlertDialog
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();
//    }

//    /**
//     * Perform the deletion of the pet in the database.
//     */
//    private void deletePet() {
//        // Only perform the delete if this is an existing pet.
//        if (mCurrentPetUri != null) {
//            // Call the ContentResolver to delete the pet at the given content URI.
//            // Pass in null for the selection and selection args because the mCurrentPetUri
//            // content URI already identifies the pet that we want.
//            int rowsDeleted = getContentResolver().delete(mCurrentPetUri, null, null);
//
//            // Show a toast message depending on whether or not the delete was successful.
//            if (rowsDeleted == 0) {
//                // If no rows were deleted, then there was an error with the delete.
//                Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
//                        Toast.LENGTH_SHORT).show();
//            } else {
//                // Otherwise, the delete was successful and we can display a toast.
//                Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
//                        Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        // Close the activity
//        finish();
//    }
}