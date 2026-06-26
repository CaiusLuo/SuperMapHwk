package ui;

import com.supermap.data.Workspace;
import com.supermap.data.WorkspaceConnectionInfo;
import com.supermap.data.WorkspaceType;
import com.supermap.ui.MapControl;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.io.File;

/**
 * @author Caius
 * @description
 * @since Created in 2026-06-26
 */
public class HwkMainFrame extends JFrame {

    private final Workspace workspace;
    private final MapControl mapControl;
    private final JTextArea logArea;

    public HwkMainFrame() {
        this.workspace = new Workspace();
        this.mapControl = new MapControl();
        this.logArea = new JTextArea();

        initFrame();
        initLayout();
        initActions();
    }

    private void initFrame() {
        setTitle("SuperMap iObjects Java 期末作业 - Caius");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openWorkspaceButton = new JButton("打开工作空间");
        openWorkspaceButton.setActionCommand("openWorkspace");

        JButton saveWorkspaceButton = new JButton("保存工作空间");
        saveWorkspaceButton.setActionCommand("saveWorkspace");

        JButton fullExtentButton = new JButton("全幅显示");
        fullExtentButton.setActionCommand("fullExtent");

        toolBar.add(openWorkspaceButton);
        toolBar.add(saveWorkspaceButton);
        toolBar.add(fullExtentButton);

        add(toolBar, BorderLayout.NORTH);

        JPanel mapPanel = new JPanel(new BorderLayout());
        mapPanel.setBorder(BorderFactory.createTitledBorder("地图显示"));
        mapPanel.add(mapControl, BorderLayout.CENTER);

        add(mapPanel, BorderLayout.CENTER);

        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("运行日志"));
        add(logScrollPane, BorderLayout.SOUTH);

        openWorkspaceButton.addActionListener(e -> openWorkspace());
        saveWorkspaceButton.addActionListener(e -> saveWorkspace());
        fullExtentButton.addActionListener(e -> fullExtent());
    }

    private void initActions() {
        // 这里后面扩展图层管理、SQL 查询、空间查询
    }

    private void openWorkspace() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 SuperMap 工作空间文件");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "SuperMap Workspace (*.smwu, *.sxwu, *.smw, *.sxw)",
                "smwu", "sxwu", "smw", "sxw"
        ));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath();

        try {
            WorkspaceConnectionInfo info = new WorkspaceConnectionInfo();
            info.setServer(path);
            info.setType(resolveWorkspaceType(path));

            boolean opened = workspace.open(info);
            if (!opened) {
                log("工作空间打开失败：" + path);
                return;
            }

            mapControl.getMap().setWorkspace(workspace);

            if (workspace.getMaps().getCount() > 0) {
                String mapName = workspace.getMaps().get(0);
                mapControl.getMap().open(mapName);
                mapControl.getMap().viewEntire();
                mapControl.getMap().refresh();

                log("工作空间打开成功：" + path);
                log("已打开地图：" + mapName);
            } else {
                log("工作空间打开成功，但没有找到地图。可以后续添加数据集到地图。");
            }
        } catch (Exception ex) {
            log("打开工作空间异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private WorkspaceType resolveWorkspaceType(String path) {
        String lower = path.toLowerCase();

        if (lower.endsWith(".smwu")) {
            return WorkspaceType.SMWU;
        }

        if (lower.endsWith(".sxwu")) {
            return WorkspaceType.SXWU;
        }

        if (lower.endsWith(".smw")) {
            return WorkspaceType.SMW;
        }

        if (lower.endsWith(".sxw")) {
            return WorkspaceType.SXW;
        }

        throw new IllegalArgumentException("不支持的工作空间类型：" + path);
    }

    private void saveWorkspace() {
        try {
            boolean saved = workspace.save();
            log(saved ? "工作空间保存成功。" : "工作空间保存失败。");
        } catch (Exception ex) {
            log("保存工作空间异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void fullExtent() {
        try {
            mapControl.getMap().viewEntire();
            mapControl.getMap().refresh();
            log("已全幅显示地图。");
        } catch (Exception ex) {
            log("全幅显示异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
    }
}
