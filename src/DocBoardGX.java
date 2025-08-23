import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;



public class DocBoardGX extends JFrame {
    private JPanel gridPanel;
    private List<File> files = new ArrayList<>();
    private String currentCategory = "Default";
    private JComboBox<String> categoryBox;
    private Map<String, List<File>> categories = new HashMap<>();
    private File storageDir;
    private File profileConfig;
    private JLabel profileLabel;
    private File profileImageFile;
    private boolean darkMode = false;
    private JComboBox<String> sortBox;
    private String username;



    // Constructor
    public DocBoardGX(String username) {
        this.username=username;

        categories = SessionManager.loadSession(username);
        if (categories.isEmpty()) {
            categories.put("Default", new ArrayList<>());
        }
            files = categories.get("Default");
            if (files == null) {
                files = new ArrayList<>();
                categories.put("Default", files);
            }
            currentCategory = "Default";
            

            

       



        // Ensure storage directory exists
        storageDir = new File(System.getProperty("user.home"), "Documents/DocBoardStorage");
        if (!storageDir.exists()) storageDir.mkdirs();


        //load profile settings
        profileConfig = new File(storageDir, "profile.txt");
        profileImageFile = new File(storageDir, "profileicon.png");


         //screen settings
        setTitle("DocBoard GX");
        setSize(1920, 1280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        applyTheme();


        

       // === TOP PANEL ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(35, 35, 35));
        topPanel.setPreferredSize(new Dimension(getWidth(), 60));
        add(topPanel, BorderLayout.NORTH);   

        // Profile Picture in Center
        profileLabel = new JLabel();
        profileLabel.setPreferredSize(new Dimension(50, 50));
        profileLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profileLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
        loadProfilePicture();

        profileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileLabel.setToolTipText("Click to change profile picture");
        //profile picture actions
        profileLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                changeProfilePicture();
            }
        });
        
        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftWrap.setOpaque(false);
        leftWrap.add(profileLabel);
        topPanel.add(leftWrap, BorderLayout.WEST);

        JPanel centerBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        centerBar.setOpaque(false);




        categoryBox = new JComboBox<>(new String[]{"Default"});
        categoryBox.addActionListener(e -> switchCategory((String) categoryBox.getSelectedItem()));

        JButton addCategoryBtn = new JButton("Add Category");
        addCategoryBtn.addActionListener(e -> addCategory());


        //  category buttons
        JButton catDocs = new JButton("Documents");
        JButton catImages = new JButton("Images");
        JButton catAll = new JButton("All");

        catDocs.addActionListener(e -> filterByExtension("doc", "docx", "pdf", "txt","xls"));
        catImages.addActionListener(e -> filterByExtension("png", "jpg", "jpeg","avif"));
        catAll.addActionListener(e -> switchCategory(currentCategory));




        // === Sorting ComboBox ===
        String[] sortOptions = {"A - Z", "Z - A", "By Extension", "By File Type"};
        sortBox = new JComboBox<>(sortOptions);
        sortBox.setPreferredSize(new Dimension(150, 25));
        sortBox.addActionListener(e -> refreshTiles());


        centerBar.add(new JLabel("Category:"));
        centerBar.add(categoryBox);
        centerBar.add(addCategoryBtn);
        centerBar.add(new JSeparator(SwingConstants.VERTICAL) {{
            setPreferredSize(new Dimension(1, 20));
        }});

        centerBar.add(catDocs);
        centerBar.add(catImages);
        centerBar.add(catAll);
        centerBar.add(new JSeparator(SwingConstants.VERTICAL) {{
            setPreferredSize(new Dimension(1, 20));
        }});



        JLabel sortLbl = new JLabel("Sort:");
        centerBar.add(sortLbl);
        // Optional sort icon if resource exists
        Icon sortIcon = safeIcon("/icons/sort.png");
        if (sortIcon != null) centerBar.add(new JLabel(sortIcon));
        centerBar.add(sortBox);

         topPanel.add(centerBar, BorderLayout.CENTER);



         // Profile/Menu icon
        JButton userMenu = new JButton(); 
        Icon menuIcon = safeIcon("/icons/menuicon.png");
        if (menuIcon != null) userMenu.setIcon(menuIcon); else userMenu.setText("☰");
        userMenu.setPreferredSize(new Dimension(40, 40));
        userMenu.setFocusPainted(false);
        userMenu.setBorderPainted(false);
        userMenu.setContentAreaFilled(false);


        //menu options 
        JPopupMenu menu = new JPopupMenu();

        //log out function
        JMenuItem logoutItem = new JMenuItem("Log Out");
        //change theme
        JMenuItem themeItem = new JMenuItem("Toggle Theme");
        //convert img to pdf
        JMenuItem imageToPdfItem = new JMenuItem("Convert Image to PDF");

        menu.add(themeItem);
        menu.add(imageToPdfItem);
        menu.addSeparator();
        menu.add(logoutItem);


         userMenu.addActionListener(e -> menu.show(userMenu, 0, userMenu.getHeight()));
        topPanel.add(new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)) {{
            setOpaque(false);
            add(userMenu);
        }}, BorderLayout.EAST);

        // Menu actions
        logoutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Logging out...");
            SessionManager.saveSession(username, categories);  // ✅ save before exit
            System.exit(0);
        });

        themeItem.addActionListener(e -> {
            darkMode = !darkMode;
            applyTheme();
            refreshTiles();
        });
        imageToPdfItem.addActionListener(e ->
            {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File imgFile = chooser.getSelectedFile();
                File pdfFile = new File(imgFile.getParent(), 
                               imgFile.getName().replaceFirst("[.][^.]+$", "") + ".pdf");
            imageToPDF(imgFile, pdfFile);
            JOptionPane.showMessageDialog(this, "PDF created: " + pdfFile.getAbsolutePath());
            }
            });
        




        // === GRID PANEL ===
        gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); 
        gridPanel.setBackground(new Color(25, 25, 25));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        add(scrollPane, BorderLayout.CENTER);

        categories.put("Default", files);

        refreshTiles();




         // Load profile from saved config
        if (profileConfig.exists()) {
        try (BufferedReader br = new BufferedReader(new FileReader(profileConfig))) {
        String path = br.readLine();
        if (path != null) {
            File savedFile = new File(path);
            if (savedFile.exists()) {
                profileImageFile = savedFile;
                setProfileImage(profileImageFile);
                }
            }
        } catch (IOException ignored) {}
        }
}


   //==THEME==
   private void applyTheme() {
        if (darkMode) {
            UIManager.put("Panel.background", new Color(25, 25, 25));
            UIManager.put("Label.foreground", Color.WHITE);
            UIManager.put("Button.background", new Color(45, 45, 45));
            UIManager.put("Button.foreground", Color.WHITE);
        } else {
            UIManager.put("Panel.background", Color.WHITE);
            UIManager.put("Label.foreground", Color.BLACK);
            UIManager.put("Button.background", new Color(230, 230, 230));
            UIManager.put("Button.foreground", Color.BLACK);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }


    // === PROFILE ===
    private void loadProfilePicture() {
        if (profileImageFile.exists()) {
            setProfileImage(profileImageFile);
        } else {
            profileLabel.setText("Add Photo");
            profileLabel.setForeground(Color.WHITE);
        }
    }

    // === PROFILE PICTURE ===
    private void changeProfilePicture() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = chooser.getSelectedFile();
        try {
                // Keep original extension
                String ext = getFileExtension(selectedFile);
                File newFile = new File(storageDir, "profile." + (ext.isEmpty() ? "png" : ext));
                Files.copy(selectedFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                profileImageFile = newFile;
                setProfileImage(profileImageFile);
                try (PrintWriter pw = new PrintWriter(new FileWriter(profileConfig))) {
                    pw.println(profileImageFile.getAbsolutePath());
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving profile picture: " + e.getMessage());
            }
    }
} 


    // === FILTER & SORT ===
    private void filterByExtension(String... exts) {
        Set<String> extSet = new HashSet<>();
        for (String e : exts) extSet.add(e.toLowerCase());
        List<File> base = categories.getOrDefault(currentCategory, new ArrayList<>());
        List<File> filtered = new ArrayList<>();
        for (File f : base) {
            if (extSet.contains(getFileExtension(f))) filtered.add(f);
        }
        files = filtered;
        refreshTiles();
    }


    private void imageToPDF(File imageFile, File outputPdf) {
    try (PDDocument doc = new PDDocument()) {
        PDPage page = new PDPage();
        doc.addPage(page);

        PDImageXObject pdImage = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), doc);
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        contentStream.drawImage(pdImage, 50, 200, 500, 400); // (x, y, width, height)
        contentStream.close();

        doc.save(outputPdf);
        System.out.println("PDF created at " + outputPdf.getAbsolutePath());
    } catch (Exception e) {
        e.printStackTrace();
    }
}



    // === CATEGORY MANAGEMENT ===
    private void switchCategory(String category) {
        currentCategory = category;
        files = categories.getOrDefault(category, new ArrayList<>());

       refreshTiles();
    }




     private void addCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter new category name:");
        if (name != null) {
            name = name.trim();
            if (!name.isEmpty() && !categories.containsKey(name)) {
                categories.put(name, new ArrayList<>());
                categoryBox.addItem(name);
                categoryBox.setSelectedItem(name);
            }
        }
    }





    private void addAddTile() {
        JPanel addTile = createTile("+", null);
        addTile.setBackground(new Color(70, 70, 70));
        addTile.setForeground(Color.WHITE);
        addTile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addDocument();
            }
        });
        gridPanel.add(addTile);
    }






    private void addDocument() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File originalFile = chooser.getSelectedFile();
            try {
                File newFile = new File(storageDir, originalFile.getName());
                Files.copy(originalFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                files.add(newFile);
                refreshTiles();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to copy file!");
            }
        }
    }




    // === TILE MANAGEMENT ===
private void refreshTiles() {
    if (gridPanel == null) return;  //  safety check
    gridPanel.removeAll();

    if (files == null) {  
        files = new ArrayList<>();  //  ensure files never null
        categories.putIfAbsent(currentCategory != null ? currentCategory : "Default", files);
    }

    // --- SORT FILES BASED ON sortBox SELECTION ---
    if (sortBox != null && sortBox.getSelectedItem() != null && !files.isEmpty()) {
        String sortOption = (String) sortBox.getSelectedItem();

        files.sort((f1, f2) -> {
            switch (sortOption) {
                case "A - Z":
                    return f1.getName().compareToIgnoreCase(f2.getName());
                case "Z - A":
                    return f2.getName().compareToIgnoreCase(f1.getName());
                case "By Extension": {
                    String ext1 = getFileExtension(f1);
                    String ext2 = getFileExtension(f2);
                    int c = ext1.compareToIgnoreCase(ext2);
                    return (c != 0) ? c : f1.getName().compareToIgnoreCase(f2.getName());
                }
                case "By File Type":
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                default:
                    return 0;
            }
        });
    }

    // --- ADD FILE TILES ---
    for (File file : files) {
        JPanel tile = createTile(file.getName(), file);
        gridPanel.add(tile);
    }

    addAddTile();  //  always add "Add" tile at end

    gridPanel.revalidate();
    gridPanel.repaint();
}




     private Icon getCustomIcon(File file) {
        String name = file.getName().toLowerCase();
        Icon icon = null;
        if (name.endsWith(".pdf")) icon = safeIcon("/icons/pdficon.png");
        else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif"))
            icon = safeIcon("/icons/imageicon.png");
        else if (name.endsWith(".txt")) icon = safeIcon("/icons/txticon.png");
        else if (name.endsWith(".doc") || name.endsWith(".docx")) icon = safeIcon("/icons/docicon.png");
        else if (name.endsWith(".xls") || name.endsWith(".xlsx")) icon = safeIcon("/icons/excelicon.png");

        if (icon == null) icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        return icon;
    }

//not currently using 
private Icon getScaledIcon(Icon icon, int width, int height) {
    if (icon instanceof ImageIcon) {
        Image img = ((ImageIcon) icon).getImage();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
    return icon;
}


    private JPanel createTile(String title, File file) {
    JPanel tile = new JPanel(new BorderLayout());
    tile.setPreferredSize(new Dimension(200, 120)); 
    tile.setBackground(new Color(40, 40, 40));
    tile.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 2, true));

   Icon icon = (file == null) ? safeIcon("/icons/blankicon.png") : getCustomIcon(file);
        if (icon instanceof ImageIcon) {
            Image img = ((ImageIcon) icon).getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
        }

    JLabel label = new JLabel("<html><center>" + title + "</center></html>", icon, SwingConstants.CENTER);
    label.setForeground(Color.WHITE);
    label.setHorizontalTextPosition(JLabel.CENTER);
    label.setVerticalTextPosition(JLabel.BOTTOM);

   
    if ("+".equals(title)) {
        label.setFont(new Font("Arial", Font.BOLD, 36)); 
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.CENTER);
    }

    tile.add(label, BorderLayout.CENTER);
    tile.setCursor(new Cursor(Cursor.HAND_CURSOR));

    if (file != null) {
        
        tile.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    openFile(file);
                }
            }
        });

        JPopupMenu menu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem removeItem = new JMenuItem("Remove");

        renameItem.addActionListener(ev -> renameFile(file));
        removeItem.addActionListener(ev -> {
            files.remove(file);
            categories.getOrDefault(currentCategory, files).remove(file);
            refreshTiles();
        });

        menu.add(renameItem);
        menu.add(removeItem);
        tile.setComponentPopupMenu(menu);

        
    }

    return tile;
}


private Icon safeIcon(String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) return new ImageIcon(url);
        } catch (Exception ignored) {}
        return null;
    }







   private String getFileExtension(File file) {
    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < name.length() - 1) {
        return name.substring(dotIndex + 1).toLowerCase();
    }
    return ""; 
}



   private void renameFile(File file) {
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", file.getName());
        if (newName == null) return;
        newName = newName.trim();
        if (newName.isEmpty()) return;

        // preserve extension if user removed it
        String currentExt = getFileExtension(file);
        if (!currentExt.isEmpty() && !newName.toLowerCase().endsWith("." + currentExt)) {
            newName += "." + currentExt;
        }
        File renamedFile = new File(storageDir, newName);
        if (file.renameTo(renamedFile)) {
            int idx = files.indexOf(file);
            if (idx >= 0) files.set(idx, renamedFile);
            refreshTiles();
        } else {
            JOptionPane.showMessageDialog(this, "Rename failed!");
        }
    }





    private void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot open file: " + file.getName());
        }
    }
  



private void setProfileImage(File file) {
    if (file != null && file.exists()) {
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        profileLabel.setIcon(new ImageIcon(img));
        profileLabel.setText(""); 
    }
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}




 class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private Map<String, String> users = new HashMap<>(); 

    public LoginWindow() {
        setTitle("Login - DocBoard GX");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2, 10, 10));
        setLocationRelativeTo(null);

        // Load saved users
        loadUsers();

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        add(loginBtn);
        add(registerBtn);

        
        loginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            if (users.containsKey(user) && users.get(user).equals(pass)) {
                JOptionPane.showMessageDialog(this, "Login successful!");
                this.dispose(); 
                new DocBoardGX(user).setVisible(true); 
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!");
            }
        });

        
        registerBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            if (!users.containsKey(user)) {
                users.put(user, pass);
                saveUsers();
                JOptionPane.showMessageDialog(this, "User registered successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "User already exists!");
            }
        });
    }

    
    private void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("users.txt"))) {
            for (String u : users.keySet()) {
                pw.println(u + ":" + users.get(u));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private void loadUsers() {
        File file = new File("users.txt");
        if (!file.exists()) return;

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }  
}

 class SessionManager {
    private static final Gson gson = new Gson();

    
    public static void saveSession(String username, Map<String, List<File>> categories) {
        try {
            File userDir = new File("storage/" + username);
            if (!userDir.exists()) userDir.mkdirs();

            File sessionFile = new File(userDir, "session.json");

            // Convert File -> String paths for JSON
            Map<String, List<String>> saveMap = new HashMap<>();
            for (String key : categories.keySet()) {
                List<String> paths = new ArrayList<>();
                for (File f : categories.get(key)) {
                    paths.add(f.getAbsolutePath());
                }
                saveMap.put(key, paths);
            }

            try (Writer writer = new FileWriter(sessionFile)) {
                gson.toJson(saveMap, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<File>> loadSession(String username) {
        try {
            File sessionFile = new File("storage/" + username + "/session.json");
            if (!sessionFile.exists()) return new HashMap<>();

            try (Reader reader = new FileReader(sessionFile)) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                Map<String, List<String>> loaded = gson.fromJson(reader, type);

               
                Map<String, List<File>> fileMap = new HashMap<>();
                for (String key : loaded.keySet()) {
                    List<File> files = new ArrayList<>();
                    for (String path : loaded.get(key)) {
                        File f = new File(path);
                        if (f.exists()) files.add(f);
                    }
                    fileMap.put(key, files);
                }
                return fileMap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }
}
