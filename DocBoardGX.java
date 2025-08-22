import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

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




    // Constructor
    public DocBoardGX() {
         // Ensure storage directory exists
        storageDir = new File(System.getProperty("user.home"), "Documents/DocBoardStorage");

       if (!storageDir.exists()) storageDir.mkdirs();

        profileConfig = new File(storageDir, "profile.txt");
        profileImageFile = new File(storageDir, "profileicon.png");


        setTitle("DocBoard GX");
        setSize(1920, 1280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        applyTheme();

       
        
        

        // === TOP PANEL ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(35, 35, 35));
        add(topPanel, BorderLayout.NORTH);   

        // Profile Picture in Center
        profileLabel = new JLabel();
        profileLabel.setForeground(Color.WHITE);
        profileLabel.setHorizontalTextPosition(JLabel.CENTER);
        profileLabel.setVerticalTextPosition(JLabel.BOTTOM);
        profileLabel.setPreferredSize(new Dimension(100, 100));
        loadProfilePicture();

        profileLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));

        profileLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                changeProfilePicture();
            }
        });

        topPanel.add(profileLabel, BorderLayout.CENTER);

        JPanel categoryAndSortPanel = new JPanel(new BorderLayout());
        categoryAndSortPanel.setBackground(new Color(35, 35, 35));

        // Category Bar at Bottom of Top Panel
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        categoryPanel.setBackground(new Color(35, 35, 35));

        categoryBox = new JComboBox<>(new String[]{"Default"});
        categoryBox.addActionListener(e -> switchCategory((String) categoryBox.getSelectedItem()));
        categoryPanel.add(new JLabel(" "));
        categoryPanel.add(categoryBox);

        JButton addCategoryBtn = new JButton("Add Category");
        addCategoryBtn.addActionListener(e -> addCategory());
        categoryPanel.add(addCategoryBtn);

        categoryAndSortPanel.add(categoryPanel, BorderLayout.WEST);

        // === GRID PANEL ===
        gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); 
        gridPanel.setBackground(new Color(25, 25, 25));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        add(scrollPane, BorderLayout.CENTER);

        categories.put("Default", files);

        refreshTiles();


        

        // Profile/Menu icon
        JButton userMenu = new JButton(new ImageIcon(getClass().getResource("/icons/menuicon.png"))); 
        //userMenu.setFocusPainted(false);
        //userMenu.setBackground(new Color(50, 50, 50));
        //userMenu.setForeground(Color.WHITE);

        JPopupMenu menu = new JPopupMenu();

        JMenuItem logoutItem = new JMenuItem("Log Out");
        logoutItem.addActionListener(e -> {
        JOptionPane.showMessageDialog(this, "Logging out...");
        System.exit(0);
        });

        JMenuItem themeItem = new JMenuItem("Toggle Theme");
        themeItem.addActionListener(e -> {
            darkMode = !darkMode;  
            applyTheme();          
            refreshTiles();
        });

        JMenuItem imageToPdfItem = new JMenuItem("Convert Image to PDF");
        imageToPdfItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Convert image to PDF coming soon!");
        });


        menu.add(logoutItem);
        menu.add(themeItem);
        menu.add(imageToPdfItem);        


        userMenu.addActionListener(e -> menu.show(userMenu, 0, userMenu.getHeight()));
        topPanel.add(userMenu,BorderLayout.EAST);            
        add(topPanel, BorderLayout.NORTH);
        

       // === Category Panel ===
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10,5));    //FlowLayout.Right
        sortPanel.setBackground(new Color(35, 35, 35));

        // Example category buttons
        JButton catDocs = new JButton("Documents");
        JButton catImages = new JButton("Images");
        JButton catAll = new JButton("All");

        sortPanel.add(catDocs);
        sortPanel.add(catImages);
        sortPanel.add(catAll);

        catDocs.addActionListener(e -> filterByExtension("doc", "docx", "pdf", "txt"));
        catImages.addActionListener(e -> filterByExtension("png", "jpg", "jpeg"));
        catAll.addActionListener(e -> switchCategory(currentCategory));



        // === Sorting ComboBox ===
        String[] sortOptions = {"A - Z", "Z - A", "By Extension", "By File Type"};
        sortBox = new JComboBox<>(sortOptions);
        sortBox.setPreferredSize(new Dimension(150, 25));
        sortBox.addActionListener(e -> refreshTiles());

        sortPanel.add(new JLabel(new ImageIcon("icons/sort.png"))); // sort icon
        sortPanel.add(sortBox);

        // Add category + sort panel to top
        categoryAndSortPanel.add(sortPanel, BorderLayout.EAST);

        topPanel.add(categoryAndSortPanel, BorderLayout.SOUTH);


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



    private void applyTheme() {
    if (darkMode) {
        UIManager.put("Panel.background", Color.DARK_GRAY);
        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("Button.background", Color.GRAY);
        UIManager.put("Button.foreground", Color.WHITE);
    } else {
        UIManager.put("Panel.background", Color.LIGHT_GRAY);
        UIManager.put("Label.foreground", Color.BLACK);
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", Color.BLACK);
    }
    SwingUtilities.updateComponentTreeUI(this);
    }


    private void loadProfilePicture() {
    if (profileImageFile.exists()) {
        ImageIcon icon = new ImageIcon(profileImageFile.getAbsolutePath());
        Image scaled = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        profileLabel.setIcon(new ImageIcon(scaled));
    } else {
        profileLabel.setText("Click to Add Photo");
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
            Files.copy(selectedFile.toPath(), profileImageFile.toPath(),
                       java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            loadProfilePicture();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving profile picture: " + e.getMessage());
        }
    }
    try (PrintWriter pw = new PrintWriter(new FileWriter(profileConfig))) {
        pw.println(profileImageFile.getAbsolutePath());
    } catch (IOException ex) {
        ex.printStackTrace();
    }
    }


    private void filterByExtension(String... exts) {
    Set<String> extSet = new HashSet<>(Arrays.asList(exts));
    List<File> filtered = new ArrayList<>();
    for (File f : categories.getOrDefault(currentCategory, files)) {
        if (extSet.contains(getFileExtension(f))) {
            filtered.add(f);
        }
    }
    files = filtered;
    refreshTiles();
}



    /*private void convertImageToPDF() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File imageFile = chooser.getSelectedFile();
        File outputFile = new File(storageDir, imageFile.getName().replaceAll("\\.[^.]+$", "") + ".pdf");
        try {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new FileOutputStream(outputFile));
            doc.open();
            com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(imageFile.getAbsolutePath());
            img.scaleToFit(500, 700);
            doc.add(img);
            doc.close();
            JOptionPane.showMessageDialog(this, "Saved as: " + outputFile.getAbsolutePath());
            refreshTiles();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "PDF conversion failed: " + e.getMessage());
        }
    }
    }*/



    // === CATEGORY MANAGEMENT ===
    private void switchCategory(String category) {
        currentCategory = category;
        files = categories.getOrDefault(category, new ArrayList<>());

       refreshTiles();
    }




    private void addCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter new category name:");
        if (name != null && !name.trim().isEmpty() && !categories.containsKey(name)) {
            categories.put(name, new ArrayList<>());
            categoryBox.addItem(name);
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
    gridPanel.removeAll();

    // --- SORT FILES BASED ON sortBox SELECTION ---
    if (sortBox != null && sortBox.getSelectedItem() != null) {
        String sortOption = (String) sortBox.getSelectedItem();

        files.sort((f1, f2) -> {
            switch (sortOption) {
                case "A - Z":
                    return f1.getName().compareToIgnoreCase(f2.getName());
                case "Z - A":
                    return f2.getName().compareToIgnoreCase(f1.getName());
                case "By Extension":
                    String ext1 = getFileExtension(f1);
                    String ext2 = getFileExtension(f2);
                    return ext1.compareToIgnoreCase(ext2);
                case "By File Type": // directories first, then files
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
            }
            return 0;
        });
    }

    // --- ADD FILE TILES ---
    for (File file : files) {
        JPanel tile = createTile(file.getName(), file);
        gridPanel.add(tile);
    }

    addAddTile(); 

    gridPanel.revalidate();
    gridPanel.repaint();
}




    private Icon getCustomIcon(File file) {
    if (file == null) {
        return new ImageIcon(getClass().getResource("/icons/blankicon.png"));
    }

    String name = file.getName().toLowerCase();
    if (name.endsWith(".pdf")) 
        return new ImageIcon(getClass().getResource("/icons/pdficon.png"));
    else if (name.endsWith(".png") || name.endsWith(".jpg")) 
        return new ImageIcon(getClass().getResource("/icons/imageicon.png"));
    else if (name.endsWith(".txt")) 
        return new ImageIcon(getClass().getResource("/icons/txticon.png"));
    else if (name.endsWith(".doc") || name.endsWith(".docx")) 
        return new ImageIcon(getClass().getResource("/icons/docicon.png"));
    else if (name.endsWith(".xls") || name.endsWith(".xlsx")) 
        return new ImageIcon(getClass().getResource("/icons/excelicon.png"));
    else 
        return FileSystemView.getFileSystemView().getSystemIcon(file); 
}


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
    tile.setPreferredSize(new Dimension(200, 100)); 
    tile.setBackground(new Color(40, 40, 40));
    tile.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 2, true));

    Icon icon = (file == null) ? null : getCustomIcon(file);
if (icon != null) {
    icon = getScaledIcon(icon, 48, 48); //  scale to 48x48
}
   /*  if (icon == null) {
        icon = new ImageIcon(getClass().getResource("\\icons\\blankicon.png")); // Default icon
    }*/

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
            refreshTiles();
        });

        menu.add(renameItem);
        menu.add(removeItem);

        tile.setComponentPopupMenu(menu);
    }

    return tile;
}





   private String getFileExtension(File file) {
    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < name.length() - 1) {
        return name.substring(dotIndex + 1).toLowerCase();
    }
    return ""; 
}


//here

    private void renameFile(File file) {
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", file.getName());
        String ext = getFileExtension(file);
        if (!newName.toLowerCase().endsWith("." + ext)) {
        newName += "." + ext;
        }
        if (newName != null && !newName.trim().isEmpty()) {
            File renamedFile = new File(storageDir, newName);
            if (file.renameTo(renamedFile)) {
                int idx = files.indexOf(file);
                files.set(idx, renamedFile);
                refreshTiles();
            } else {
                JOptionPane.showMessageDialog(this, "Rename failed!");
            }
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
        Image img = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
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
                new DocBoardGX().setVisible(true); 
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
