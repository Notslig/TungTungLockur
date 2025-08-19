import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

public class DocBoardGX extends JFrame {
    private JPanel gridPanel;
    private List<File> files = new ArrayList<>();
    private String currentCategory = "Default";
    private JComboBox<String> categoryBox;
    private Map<String, List<File>> categories = new HashMap<>();
    private File storageDir;
    private File profileConfig = new File(storageDir, "profile.txt");

    // Profile
    private JLabel profileLabel;
    private File profileImageFile = null;

    public DocBoardGX() {
        setTitle("DocBoard GX");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Ensure storage directory exists
        storageDir = new File(System.getProperty("C:\\Users\\IND\\Documents"), "DocBoardStorage");
       if (!storageDir.exists()) {
        if (storageDir.mkdirs()) {
            System.out.println("Storage folder created at: " + storageDir.getAbsolutePath());
        } else {
            System.out.println("Failed to create storage folder.");
        }
    } else {
        System.out.println("Storage folder already exists at: " + storageDir.getAbsolutePath());
    }

        // Dark Theme
        UIManager.put("Panel.background", new Color(25, 25, 25));
        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("Button.background", new Color(45, 45, 45));
        UIManager.put("Button.foreground", Color.WHITE);

        // === TOP PANEL ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(35, 35, 35));

        // Profile Picture in Center
        profileLabel = new JLabel("Click to set Profile", SwingConstants.CENTER);
        profileLabel.setForeground(Color.WHITE);
        profileLabel.setHorizontalTextPosition(JLabel.CENTER);
        profileLabel.setVerticalTextPosition(JLabel.BOTTOM);
        profileLabel.setPreferredSize(new Dimension(150, 150));
        profileLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));

        profileLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                changeProfilePicture();
            }
        });

        topPanel.add(profileLabel, BorderLayout.CENTER);

        // Category Bar at Bottom of Top Panel
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        categoryPanel.setBackground(new Color(35, 35, 35));

        categoryBox = new JComboBox<>(new String[]{"Default"});
        categoryBox.addActionListener(e -> switchCategory((String) categoryBox.getSelectedItem()));
        categoryPanel.add(new JLabel("Category: "));
        categoryPanel.add(categoryBox);

        JButton addCategoryBtn = new JButton("Add Category");
        addCategoryBtn.addActionListener(e -> addCategory());
        categoryPanel.add(addCategoryBtn);

        topPanel.add(categoryPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // === GRID PANEL ===
        gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); // 3 tiles per row
        gridPanel.setBackground(new Color(25, 25, 25));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

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

    private void refreshTiles() {
        gridPanel.removeAll();

        for (File file : files) {
            JPanel tile = createTile(file.getName(), file);
            gridPanel.add(tile);
        }

        addAddTile(); // Always at end

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createTile(String title, File file) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setPreferredSize(new Dimension(200, 100)); // Rectangular size
        tile.setBackground(new Color(40, 40, 40));
        tile.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 2, true));

        // Custom icon based on file type
        Icon icon = UIManager.getIcon("FileView.fileIcon");
        if (file != null) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".pdf")) icon = UIManager.getIcon("FileView.hardDriveIcon");
            else if (name.endsWith(".png") || name.endsWith(".jpg")) icon = UIManager.getIcon("FileView.directoryIcon");
            else icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        }

        JLabel label = new JLabel("<html><center>" + title + "</center></html>", icon, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        tile.add(label, BorderLayout.CENTER);

        tile.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (file != null) {
            // Left click = open
            tile.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        openFile(file);
                    }
                }
            });

            // Right click = context menu
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
    int lastIndex = name.lastIndexOf(".");
    return (lastIndex == -1) ? "" : name.substring(lastIndex);
   }


    private void renameFile(File file) {
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", file.getName());
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

    // === PROFILE PICTURE ===
   private void changeProfilePicture() {
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = chooser.getSelectedFile();

        try {
            // Copy chosen image to storage
            File newFile = new File(storageDir, "profile" + getFileExtension(selectedFile));
            Files.copy(selectedFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            profileImageFile = newFile;

            // Save path in config file
            try (FileWriter fw = new FileWriter(profileConfig)) {
                fw.write(profileImageFile.getAbsolutePath());
            }

            setProfileImage(profileImageFile);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save profile picture!");
        }
    }
}
private void setProfileImage(File file) {
    if (file != null && file.exists()) {
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        Image img = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        profileLabel.setIcon(new ImageIcon(img));
        profileLabel.setText(""); // remove "Click to set" text
    }
}



    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new DocBoardGX().setVisible(true));
    }
}
