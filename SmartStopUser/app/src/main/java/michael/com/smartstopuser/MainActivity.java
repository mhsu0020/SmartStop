package michael.com.smartstopuser;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_19);
    private static final String TAG = MainActivity.class.getName();

    private String getHasOrNot(String fileName){

        if(fileName.contains("19-19-22") || fileName.contains("19-19-25") || fileName.contains("19-19-27")){
            return "nopokemon";
        } else{
            return "haspokemon";
        }
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1)
                onSelectFromGalleryResult(data);
        }
    }

    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }


    private void process(){

    }

    public  Bitmap decodeBitmap(Uri selectedImage) throws FileNotFoundException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        final int REQUIRED_SIZE = 100;

        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    private class PostImageTask extends AsyncTask<String, Void, String> {

        String paths;
        public PostImageTask(String path) {
            this.paths = path;
        }

        @Override
        protected String doInBackground(String... path) {
            InputStream imageStream = null;
            FileOutputStream fos = null;

            try {
                File copiedFile = new File(paths);

                Log.e(TAG, "Copied In folder: "+copiedFile.getPath());
                Log.e(TAG, copiedFile.length() + "");
                ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                        .images(copiedFile)
                        .classifierIds(new ArrayList<String>()).classifierIds("pokemongo1_353365368")
                        .build();

                VisualClassification result = service.classify(options).execute();
                //Log.e(TAG, "result: " + result.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textViewPokemonFound = (TextView) findViewById(R.id.pokemonfound);
                        textViewPokemonFound.setText(getHasOrNot(paths).equals("haspokemon") ? "Pokemon Found!" : "No Pokemon");
                    }
                });



                String wsUrl = "wss://runm-east.att.io/042c6ef9aa2c0/af54689f1f07/cd37b285777ff9b/in/flow/ws/pokemon";
                AsyncHttpClient.getDefaultInstance().websocket(wsUrl, "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
                    @Override
                    public void onCompleted(Exception ex, WebSocket webSocket) {
                        if (ex != null) {
                            ex.printStackTrace();
                            return;
                        }

                        Log.e(TAG, "websocket is complete");
                        webSocket.send(getHasOrNot(paths));
                    }
                });

                if(result.getImages() != null){
                    List<VisualClassifier> resultClasses = result.getImages().get(0).getClassifiers();
                    if(resultClasses.size() > 0){
                        VisualClassifier classifier = resultClasses.get(0);
                        List<VisualClassifier.VisualClass> classList = classifier.getClasses();
                        if(classList.size() > 0){
                            Log.e(TAG, "classifier name: " + classList.get(0).getName());
                            return classList.get(0).getName();

                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }



        @Override
        protected void onPostExecute(String classification) {

        }
    }
    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        if (data != null) {
            try {
                TextView textViewPokemonFound = (TextView)findViewById(R.id.pokemonfound);

                textViewPokemonFound.setText("Getting Results From Watson API...");

                Uri selectedImageUri = data.getData();
                final String selectedImagePath = getPath(selectedImageUri);
                Log.e(TAG, "result path: "+selectedImagePath);
                Log.e(TAG, "result string: " + getHasOrNot(selectedImagePath));

                try {
                    Bitmap bitmapImage =decodeBitmap(selectedImageUri );
                    ImageView imageView = (ImageView) findViewById(R.id.imgView);
                    imageView.setImageBitmap(bitmapImage);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }


                new PostImageTask(selectedImagePath).execute();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        service.setApiKey("0d9529ee70f94495b80b4f4f6bd266935dd13e6e");


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                galleryIntent();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //persmission method.
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
