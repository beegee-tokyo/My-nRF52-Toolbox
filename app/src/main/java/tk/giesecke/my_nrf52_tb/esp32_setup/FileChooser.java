package tk.giesecke.my_nrf52_tb.esp32_setup;

import android.app.Activity;
import android.app.Dialog;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.util.Arrays;

class FileChooser {
    private static final String PARENT_DIR = "..";

    private final Activity activity;
    private ListView list;
    private Dialog dialog;
    private File currentPath;

    // filter on file extension
    private String extension = "bin";
    void setExtension() {
        this.extension = "bin".toLowerCase();
    }

    // file selection event handling
    public interface FileSelectedListener {
        void fileSelected(File file);
    }

    void setFileListener(FileSelectedListener fileListener) {
        this.fileListener = fileListener;
    }
    private FileSelectedListener fileListener;

    FileChooser(Activity activity) {
        this.activity = activity;
        dialog = new Dialog(activity);
        list = new ListView(activity);
        list.setOnItemClickListener((parent, view, which, id) -> {
                String fileChosen = (String) list.getItemAtPosition(which);
                File chosenFile = getChosenFile(fileChosen);
                if (chosenFile.isDirectory()) {
                    refresh(chosenFile);
                } else {
                    if (fileListener != null) {
                        fileListener.fileSelected(chosenFile);
                    }
                    dialog.dismiss();
                }
        });
        dialog.setContentView(list);
        Window thisWindow = dialog.getWindow();
        if (thisWindow != null) {
            thisWindow.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
//        dialog.getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        File fwFile = new File(Environment.getExternalStorageDirectory() + "/SRC_Portal/");
        refresh(fwFile);
    }

    void showDialog() {
        dialog.show();
    }


    /**
     * Sort, filter and display the files for the given path.
     */
    private void refresh(File path) {
        this.currentPath = path;
        if (path.exists()) {
            File[] dirs = path.listFiles(file -> {
                    // We don't want to see folders
                    return false;
//                    return (file.isDirectory() && file.canRead());
            });
            File[] files = path.listFiles(file -> {
                    if (!file.isDirectory()) {
                        if (!file.canRead()) {
                            return false;
                        } else if (extension == null) {
                            return true;
                        } else {
                            if (file.getName().startsWith("firmware_gateway")) {
                                return file.getName().toLowerCase().endsWith(extension);
                            }
                        if (file.getName().startsWith("gateway-wrover")) {
                            return file.getName().toLowerCase().endsWith(extension);
                        }
                        }
                    }
                    return false;
            });

            // convert to an array
            int i = 0;
            String[] fileList;
            if (path.getParentFile() == null) {
                fileList = new String[dirs.length + files.length];
            } else {
                fileList = new String[dirs.length + files.length + 1];
                fileList[i++] = PARENT_DIR;
            }
            Arrays.sort(dirs);
            Arrays.sort(files);
            for (File dir : dirs) { fileList[i++] = dir.getName(); }
            for (File file : files ) { fileList[i++] = file.getName(); }

            // refresh the user interface
            dialog.setTitle(currentPath.getPath());
            //noinspection unchecked,NullableProblems
            list.setAdapter(new ArrayAdapter(activity,
                    android.R.layout.simple_list_item_1, fileList) {
                @Override public View getView(int pos, View view, ViewGroup parent) {
                    view = super.getView(pos, view, parent);
                    ((TextView) view).setSingleLine(true);
                    return view;
                }
            });
        }
    }


    /**
     * Convert a relative filename into an actual File object.
     */
    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(PARENT_DIR)) {
            return currentPath.getParentFile();
        } else {
            return new File(currentPath, fileChosen);
        }
    }
}