package ui;

import com.supermap.data.CursorType;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetVector;
import com.supermap.data.Datasets;
import com.supermap.data.Datasource;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.Datasources;
import com.supermap.data.EncodeType;
import com.supermap.data.EngineType;
import com.supermap.data.FieldInfos;
import com.supermap.data.Geometry;
import com.supermap.data.QueryParameter;
import com.supermap.data.Recordset;
import com.supermap.data.SpatialQueryMode;
import com.supermap.data.Workspace;
import com.supermap.data.WorkspaceConnectionInfo;
import com.supermap.data.WorkspaceType;
import com.supermap.mapping.Layer;
import com.supermap.mapping.Layers;
import com.supermap.mapping.Selection;
import com.supermap.ui.Action;
import com.supermap.ui.MapControl;
import com.supermap.ui.SelectionMode;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 课程作业 MVP：一个窗口，把 SuperMap 常用操作摆出来。
 */
public class HwkMainFrame extends JFrame {

    private static final int MAX_QUERY_ROWS = 1000;

    private final Workspace workspace = new Workspace();
    private final MapControl mapControl = new MapControl();

    private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("工作空间");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
    private final JTree dataTree = new JTree(treeModel);

    private final DefaultListModel<Layer> layerModel = new DefaultListModel<>();
    private final JList<Layer> layerList = new JList<>(layerModel);

    private final DefaultTableModel resultModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(resultModel);
    private final JTextArea logArea = new JTextArea(6, 40);

    private String currentWorkspacePath;

    public HwkMainFrame() {
        mapControl.getMap().setWorkspace(workspace);
        mapControl.setSelectionMode(SelectionMode.INTERSECT);

        initFrame();
        initLayout();
        initActions();
        refreshAll();
    }

    private void initFrame() {
        setTitle("SuperMap iObjects Java 期末作业 - Caius");
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mapControl.dispose();
                workspace.dispose();
            }
        });
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        JToolBar dataBar = toolbar();
        dataBar.add(button("打开工作空间", () -> openWorkspace()));
        dataBar.add(button("保存工作空间", () -> saveWorkspace()));
        dataBar.add(button("关闭工作空间", () -> closeWorkspace()));
        dataBar.addSeparator();
        dataBar.add(button("打开地图", () -> chooseAndOpenMap()));
        dataBar.add(button("打开数据源", () -> openDatasource()));
        dataBar.add(button("关闭数据源", () -> closeSelectedDatasource()));
        dataBar.addSeparator();
        dataBar.add(button("添加数据集到地图", () -> addDatasetToMap()));
        dataBar.add(button("复制数据集", () -> copySelectedDataset()));
        dataBar.add(button("删除数据集", () -> deleteSelectedDataset()));

        JToolBar mapBar = toolbar();
        mapBar.add(button("全幅显示", () -> fullExtent()));
        mapBar.add(button("放大", () -> setMapAction(Action.ZOOMIN, "已切换为放大模式。")));
        mapBar.add(button("缩小", () -> setMapAction(Action.ZOOMOUT, "已切换为缩小模式。")));
        mapBar.add(button("平移", () -> setMapAction(Action.PAN, "已切换为平移模式。")));
        mapBar.add(button("选择", () -> enableSelectMode()));
        mapBar.add(button("刷新地图", () -> refreshMap(true)));
        mapBar.addSeparator();
        mapBar.add(button("图层显示/隐藏", () -> toggleLayerVisible()));
        mapBar.add(button("图层上移", () -> moveSelectedLayer(true)));
        mapBar.add(button("图层下移", () -> moveSelectedLayer(false)));
        mapBar.addSeparator();
        mapBar.add(button("SQL 查询", () -> showSqlQueryDialog()));
        mapBar.add(button("空间查询", () -> showSpatialQueryDialog()));
        mapBar.add(button("统计信息", () -> showStatistics()));

        JPanel north = new JPanel(new GridLayout(2, 1));
        north.add(dataBar);
        north.add(mapBar);
        add(north, BorderLayout.NORTH);

        dataTree.setRootVisible(true);
        dataTree.setShowsRootHandles(true);
        dataTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JScrollPane treePane = new JScrollPane(dataTree);
        treePane.setPreferredSize(new Dimension(260, 600));
        treePane.setBorder(BorderFactory.createTitledBorder("数据源 / 数据集"));

        JPanel mapPanel = new JPanel(new BorderLayout());
        mapPanel.setBorder(BorderFactory.createTitledBorder("地图显示"));
        mapPanel.add(mapControl, BorderLayout.CENTER);

        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Layer) {
                    Layer layer = (Layer) value;
                    Dataset dataset = layer.getDataset();
                    String name = layer.getCaption();
                    if (name == null || name.trim().isEmpty()) {
                        name = layer.getName();
                    }
                    label.setText((layer.isVisible() ? "[显示] " : "[隐藏] ") + name
                            + (dataset == null ? "" : " - " + dataset.getName()));
                }
                return label;
            }
        });

        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTabbedPane rightTabs = new JTabbedPane(SwingConstants.TOP);
        rightTabs.addTab("图层", new JScrollPane(layerList));
        rightTabs.addTab("查询结果", new JScrollPane(resultTable));
        rightTabs.setPreferredSize(new Dimension(360, 600));

        JSplitPane mapAndSide = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, rightTabs);
        mapAndSide.setResizeWeight(1.0);
        mapAndSide.setDividerLocation(850);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePane, mapAndSide);
        main.setDividerLocation(270);
        add(main, BorderLayout.CENTER);

        logArea.setEditable(false);
        JScrollPane logPane = new JScrollPane(logArea);
        logPane.setPreferredSize(new Dimension(100, 130));
        logPane.setBorder(BorderFactory.createTitledBorder("运行日志"));
        add(logPane, BorderLayout.SOUTH);
    }

    private void initActions() {
        mapControl.addGeometrySelectedListener(e -> log("已选择对象数：" + e.getCount()));
        mapControl.addGeometrySelectChangedListener(e -> log("选择集变化，对象数：" + e.getCount()));
    }

    private JToolBar toolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        return bar;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void openWorkspace() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 SuperMap 工作空间文件");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "SuperMap Workspace (*.smwu, *.sxwu, *.smw, *.sxw)",
                "smwu", "sxwu", "smw", "sxw"
        ));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String path = chooser.getSelectedFile().getAbsolutePath();
        try {
            mapControl.getMap().close();
            workspace.close();

            WorkspaceConnectionInfo info = new WorkspaceConnectionInfo();
            info.setServer(path);
            info.setType(resolveWorkspaceType(path));

            if (!workspace.open(info)) {
                currentWorkspacePath = null;
                refreshAll();
                log("工作空间打开失败：" + path);
                return;
            }

            currentWorkspacePath = path;
            mapControl.getMap().setWorkspace(workspace);
            log("工作空间打开成功：" + path);

            if (workspace.getMaps().getCount() > 0) {
                openMap(workspace.getMaps().get(0));
            } else {
                refreshAll();
                log("工作空间没有保存的地图，可以直接添加数据集到地图。");
            }
        } catch (Exception ex) {
            logException("打开工作空间异常", ex);
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
        if (currentWorkspacePath == null) {
            log("没有已打开的工作空间，跳过保存。");
            return;
        }

        try {
            log(workspace.save() ? "工作空间保存成功。" : "工作空间保存失败。");
        } catch (Exception ex) {
            logException("保存工作空间异常", ex);
        }
    }

    private void closeWorkspace() {
        try {
            mapControl.getMap().close();
            workspace.close();
            currentWorkspacePath = null;
            mapControl.getMap().setWorkspace(workspace);
            clearResultTable();
            refreshAll();
            refreshMap(false);
            log("已关闭工作空间。");
        } catch (Exception ex) {
            logException("关闭工作空间异常", ex);
        }
    }

    private void chooseAndOpenMap() {
        int count = workspace.getMaps().getCount();
        if (count == 0) {
            log("当前工作空间没有可打开的地图。");
            return;
        }

        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = workspace.getMaps().get(i);
        }

        String mapName = (String) JOptionPane.showInputDialog(
                this, "选择地图", "打开地图",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]
        );
        if (mapName != null) {
            openMap(mapName);
        }
    }

    private void openMap(String mapName) {
        try {
            mapControl.getMap().close();
            mapControl.getMap().setWorkspace(workspace);
            if (!mapControl.getMap().open(mapName)) {
                refreshLayerList();
                log("地图打开失败：" + mapName);
                return;
            }

            mapControl.getMap().viewEntire();
            refreshMap(false);
            refreshLayerList();
            log("已打开地图：" + mapName);
        } catch (Exception ex) {
            logException("打开地图异常", ex);
        }
    }

    private void openDatasource() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 UDB/UDBX 数据源");
        chooser.setFileFilter(new FileNameExtensionFilter("SuperMap Datasource (*.udb, *.udbx)", "udb", "udbx"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath();
        try {
            DatasourceConnectionInfo info = new DatasourceConnectionInfo();
            info.setServer(path);
            info.setEngineType(resolveDatasourceEngine(path));
            info.setAlias(availableDatasourceAlias(file));

            Datasource datasource = workspace.getDatasources().open(info);
            if (datasource == null) {
                log("数据源打开失败：" + path);
                return;
            }

            refreshTree();
            log("数据源打开成功：" + datasource.getAlias());
        } catch (Exception ex) {
            logException("打开数据源异常", ex);
        }
    }

    private EngineType resolveDatasourceEngine(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".udbx")) {
            return EngineType.UDBX;
        }
        if (lower.endsWith(".udb")) {
            return EngineType.UDB;
        }
        throw new IllegalArgumentException("只支持 .udb / .udbx 数据源：" + path);
    }

    private String availableDatasourceAlias(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        base = base.replace(' ', '_').replace('-', '_');
        if (base.trim().isEmpty()) {
            base = "datasource";
        }

        Datasources datasources = workspace.getDatasources();
        if (!datasources.contains(base)) {
            return base;
        }

        for (int i = 1; ; i++) {
            String alias = base + "_" + i;
            if (!datasources.contains(alias)) {
                return alias;
            }
        }
    }

    private void closeSelectedDatasource() {
        Datasource datasource = selectedDatasource();
        if (datasource == null) {
            log("请先在左侧选择一个数据源。");
            return;
        }

        String alias = datasource.getAlias();
        try {
            removeLayersForDatasource(datasource);
            boolean closed = workspace.getDatasources().close(alias);
            refreshTree();
            refreshLayerList();
            refreshMap(false);
            log(closed ? "数据源已关闭：" + alias : "数据源关闭失败：" + alias);
        } catch (Exception ex) {
            logException("关闭数据源异常", ex);
        }
    }

    private void addDatasetToMap() {
        Dataset dataset = selectedDataset();
        if (dataset == null) {
            log("请先在左侧选择一个数据集。");
            return;
        }

        try {
            Layer layer = mapControl.getMap().getLayers().add(dataset, true);
            if (layer == null) {
                log("添加图层失败：" + dataset.getName());
                return;
            }

            layer.setSelectable(true);
            mapControl.getMap().viewEntire();
            refreshMap(false);
            refreshLayerList();
            log("已添加数据集到地图：" + dataset.getName());
        } catch (Exception ex) {
            logException("添加数据集到地图异常", ex);
        }
    }

    private void copySelectedDataset() {
        Dataset dataset = selectedDataset();
        if (dataset == null) {
            log("请先在左侧选择一个数据集。");
            return;
        }

        Datasets datasets = dataset.getDatasource().getDatasets();
        String suggested = datasets.getAvailableDatasetName(dataset.getName() + "_copy");
        String name = JOptionPane.showInputDialog(this, "复制后的数据集名", suggested);
        if (name == null) {
            return;
        }

        name = name.trim();
        if (name.isEmpty()) {
            log("复制取消：数据集名为空。");
            return;
        }
        if (!datasets.isAvailableDatasetName(name)) {
            JOptionPane.showMessageDialog(this, "数据集名不可用：" + name);
            return;
        }

        try {
            Dataset copied = dataset.getDatasource().copyDataset(dataset, name, EncodeType.NONE);
            refreshTree();
            log(copied == null ? "复制数据集失败：" + dataset.getName() : "复制数据集成功：" + copied.getName());
        } catch (Exception ex) {
            logException("复制数据集异常", ex);
        }
    }

    private void deleteSelectedDataset() {
        Dataset dataset = selectedDataset();
        if (dataset == null) {
            log("请先在左侧选择一个数据集。");
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
                this,
                "确定删除数据集：" + dataset.getName() + "？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        String name = dataset.getName();
        Datasource datasource = dataset.getDatasource();
        try {
            mapControl.getMap().getLayers().removeByDataset(dataset);
            boolean deleted = datasource.getDatasets().delete(name);
            refreshTree();
            refreshLayerList();
            refreshMap(false);
            log(deleted ? "数据集已删除：" + name : "数据集删除失败：" + name);
        } catch (Exception ex) {
            logException("删除数据集异常", ex);
        }
    }

    private void fullExtent() {
        try {
            mapControl.getMap().viewEntire();
            refreshMap(false);
            log("已全幅显示地图。");
        } catch (Exception ex) {
            logException("全幅显示异常", ex);
        }
    }

    private void setMapAction(Action action, String message) {
        try {
            mapControl.setAction(action);
            log(message);
        } catch (Exception ex) {
            logException("切换地图操作异常", ex);
        }
    }

    private void enableSelectMode() {
        try {
            mapControl.setSelectionMode(SelectionMode.INTERSECT);
            mapControl.setAction(Action.SELECT);
            log("选择模式已启用。选中要素会由 SuperMap 自动高亮。");
        } catch (Exception ex) {
            logException("启用选择模式异常", ex);
        }
    }

    private void refreshMap(boolean writeLog) {
        try {
            mapControl.getMap().refresh();
            refreshLayerList();
            if (writeLog) {
                log("地图已刷新。");
            }
        } catch (Exception ex) {
            logException("刷新地图异常", ex);
        }
    }

    private void toggleLayerVisible() {
        Layer layer = selectedLayer();
        if (layer == null) {
            log("请先选择一个图层。");
            return;
        }

        try {
            layer.setVisible(!layer.isVisible());
            refreshMap(false);
            log("图层可见性已切换：" + layerDisplayName(layer));
        } catch (Exception ex) {
            logException("切换图层显示异常", ex);
        }
    }

    private void moveSelectedLayer(boolean up) {
        int index = layerList.getSelectedIndex();
        if (index < 0) {
            log("请先选择一个图层。");
            return;
        }

        try {
            Layers layers = mapControl.getMap().getLayers();
            boolean moved = up ? layers.moveUp(index) : layers.moveDown(index);
            refreshMap(false);
            if (moved && layerModel.getSize() > 0) {
                int newIndex = up ? Math.max(0, index - 1) : Math.min(layerModel.getSize() - 1, index + 1);
                layerList.setSelectedIndex(newIndex);
            }
            log(moved ? "图层顺序已调整。" : "图层顺序调整失败。");
        } catch (Exception ex) {
            logException("调整图层顺序异常", ex);
        }
    }

    private void showSqlQueryDialog() {
        List<DatasetItem> items = vectorDatasetItems();
        if (items.isEmpty()) {
            log("没有可查询的矢量数据集。");
            return;
        }

        JComboBox<DatasetItem> datasetBox = new JComboBox<>(items.toArray(new DatasetItem[0]));
        selectCurrentDataset(datasetBox);
        JTextField conditionField = new JTextField("SmID > 0", 26);

        JPanel panel = formPanel();
        addFormRow(panel, 0, "数据集", datasetBox);
        addFormRow(panel, 1, "SQL 条件", conditionField);

        int answer = JOptionPane.showConfirmDialog(this, panel, "SQL 查询", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }

        DatasetItem item = (DatasetItem) datasetBox.getSelectedItem();
        if (item == null) {
            return;
        }

        String condition = conditionField.getText().trim();
        if (condition.isEmpty()) {
            condition = "SmID > 0";
        }

        QueryParameter parameter = new QueryParameter();
        try {
            parameter.setCursorType(CursorType.STATIC);
            parameter.setHasGeometry(false);
            parameter.setAttributeFilter(condition);

            Recordset recordset = item.dataset.query(parameter);
            showRecordset(recordset, "SQL 查询");
        } catch (Exception ex) {
            clearResultTable();
            log("SQL 查询失败，条件未通过：" + condition + "；" + ex.getMessage());
        } finally {
            parameter.dispose();
        }
    }

    private void showSpatialQueryDialog() {
        List<DatasetItem> items = vectorDatasetItems();
        if (items.isEmpty()) {
            log("没有可查询的矢量数据集。");
            return;
        }

        Geometry selectedGeometry = selectedGeometryFromMap();
        if (selectedGeometry == null) {
            JOptionPane.showMessageDialog(this, "请先在地图上选择一个要素作为空间查询对象。");
            log("空间查询取消：地图上没有可用的选中要素几何。");
            return;
        }

        JComboBox<DatasetItem> datasetBox = new JComboBox<>(items.toArray(new DatasetItem[0]));
        selectCurrentDataset(datasetBox);

        int answer = JOptionPane.showConfirmDialog(this, datasetBox, "选择空间查询目标数据集", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            selectedGeometry.dispose();
            return;
        }

        DatasetItem item = (DatasetItem) datasetBox.getSelectedItem();
        if (item == null) {
            selectedGeometry.dispose();
            return;
        }

        QueryParameter parameter = new QueryParameter();
        try {
            parameter.setCursorType(CursorType.STATIC);
            parameter.setHasGeometry(false);
            parameter.setSpatialQueryObject(selectedGeometry);
            parameter.setSpatialQueryMode(SpatialQueryMode.INTERSECT);

            Recordset recordset = item.dataset.query(parameter);
            showRecordset(recordset, "空间查询");
        } catch (Exception ex) {
            clearResultTable();
            log("空间查询失败：" + ex.getMessage());
        } finally {
            parameter.dispose();
            selectedGeometry.dispose();
        }
    }

    private void showStatistics() {
        int datasourceCount = workspace.getDatasources().getCount();
        int datasetCount = 0;
        for (int i = 0; i < datasourceCount; i++) {
            datasetCount += workspace.getDatasources().get(i).getDatasets().getCount();
        }

        String recordCount = "未选择矢量数据集";
        Dataset dataset = selectedDataset();
        if (dataset instanceof DatasetVector) {
            try {
                recordCount = String.valueOf(((DatasetVector) dataset).getRecordCount());
            } catch (Exception ex) {
                recordCount = "读取失败：" + ex.getMessage();
            }
        }

        String message = "工作空间地图数：" + workspace.getMaps().getCount()
                + "\n数据源数：" + datasourceCount
                + "\n数据集数：" + datasetCount
                + "\n当前地图图层数：" + mapControl.getMap().getLayers().getCount()
                + "\n选中数据集记录数：" + recordCount;

        JOptionPane.showMessageDialog(this, message, "统计信息", JOptionPane.INFORMATION_MESSAGE);
        log("已显示统计信息。");
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(420, 90));
        return panel;
    }

    private void addFormRow(JPanel panel, int row, String label, Component field) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.insets = new Insets(6, 6, 6, 8);
        left.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(6, 0, 6, 6);
        panel.add(field, right);
    }

    private void showRecordset(Recordset recordset, String title) {
        if (recordset == null) {
            clearResultTable();
            log(title + "失败：没有返回记录集。");
            return;
        }

        try {
            FieldInfos fields = recordset.getFieldInfos();
            Vector<String> columns = new Vector<>();
            for (int i = 0; i < fields.getCount(); i++) {
                columns.add(fields.get(i).getName());
            }

            Vector<Vector<Object>> rows = new Vector<>();
            int total = recordset.getRecordCount();
            int shown = 0;
            for (recordset.moveFirst(); !recordset.isEOF() && shown < MAX_QUERY_ROWS; recordset.moveNext()) {
                Vector<Object> row = new Vector<>();
                for (int i = 0; i < fields.getCount(); i++) {
                    row.add(recordset.getFieldValue(i));
                }
                rows.add(row);
                shown++;
            }

            resultModel.setDataVector(rows, columns);
            log(title + "结果：" + total + " 条，表格显示 " + shown + " 条。");
        } catch (Exception ex) {
            clearResultTable();
            log(title + "结果渲染失败：" + ex.getMessage());
        } finally {
            recordset.dispose();
        }
    }

    private Geometry selectedGeometryFromMap() {
        try {
            Selection[] selections = mapControl.getMap().findSelection(true);
            if (selections == null) {
                return null;
            }

            for (Selection selection : selections) {
                if (selection != null && selection.getCount() > 0) {
                    Recordset recordset = null;
                    try {
                        recordset = selection.toRecordset();
                        if (recordset != null && !recordset.isEmpty() && recordset.moveFirst() && !recordset.isEOF()) {
                            Geometry geometry = recordset.getGeometry();
                            if (geometry != null && !geometry.isEmpty()) {
                                return geometry;
                            }
                            if (geometry != null) {
                                geometry.dispose();
                            }
                        }
                    } finally {
                        if (recordset != null) {
                            recordset.dispose();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log("读取地图选择集失败：" + ex.getMessage());
        }
        return null;
    }

    private List<DatasetItem> vectorDatasetItems() {
        List<DatasetItem> items = new ArrayList<>();
        Datasources datasources = workspace.getDatasources();
        for (int i = 0; i < datasources.getCount(); i++) {
            Datasource datasource = datasources.get(i);
            Datasets datasets = datasource.getDatasets();
            for (int j = 0; j < datasets.getCount(); j++) {
                Dataset dataset = datasets.get(j);
                if (dataset instanceof DatasetVector) {
                    items.add(new DatasetItem((DatasetVector) dataset));
                }
            }
        }
        return items;
    }

    private void selectCurrentDataset(JComboBox<DatasetItem> box) {
        Dataset selected = selectedDataset();
        if (!(selected instanceof DatasetVector)) {
            return;
        }

        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).dataset.equals(selected)) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    private void refreshAll() {
        refreshTree();
        refreshLayerList();
    }

    private void refreshTree() {
        treeRoot.removeAllChildren();
        Datasources datasources = workspace.getDatasources();
        for (int i = 0; i < datasources.getCount(); i++) {
            Datasource datasource = datasources.get(i);
            DefaultMutableTreeNode datasourceNode = new DefaultMutableTreeNode(new TreeItem(datasource));
            Datasets datasets = datasource.getDatasets();
            for (int j = 0; j < datasets.getCount(); j++) {
                datasourceNode.add(new DefaultMutableTreeNode(new TreeItem(datasets.get(j))));
            }
            treeRoot.add(datasourceNode);
        }
        treeModel.reload();
        for (int i = 0; i < dataTree.getRowCount(); i++) {
            dataTree.expandRow(i);
        }
    }

    private void refreshLayerList() {
        int oldIndex = layerList.getSelectedIndex();
        layerModel.clear();
        Layers layers = mapControl.getMap().getLayers();
        for (int i = 0; i < layers.getCount(); i++) {
            layerModel.addElement(layers.get(i));
        }
        if (layerModel.getSize() > 0) {
            layerList.setSelectedIndex(Math.max(0, Math.min(oldIndex, layerModel.getSize() - 1)));
        }
    }

    private void clearResultTable() {
        resultModel.setRowCount(0);
        resultModel.setColumnCount(0);
    }

    private Dataset selectedDataset() {
        TreeItem item = selectedTreeItem();
        return item == null ? null : item.dataset;
    }

    private Datasource selectedDatasource() {
        TreeItem item = selectedTreeItem();
        if (item == null) {
            return null;
        }
        return item.dataset == null ? item.datasource : item.dataset.getDatasource();
    }

    private TreeItem selectedTreeItem() {
        TreePath path = dataTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        if (!(node instanceof DefaultMutableTreeNode)) {
            return null;
        }
        Object value = ((DefaultMutableTreeNode) node).getUserObject();
        return value instanceof TreeItem ? (TreeItem) value : null;
    }

    private Layer selectedLayer() {
        return layerList.getSelectedValue();
    }

    private void removeLayersForDatasource(Datasource datasource) {
        Datasets datasets = datasource.getDatasets();
        for (int i = 0; i < datasets.getCount(); i++) {
            mapControl.getMap().getLayers().removeByDataset(datasets.get(i));
        }
    }

    private String layerDisplayName(Layer layer) {
        String name = layer.getCaption();
        if (name == null || name.trim().isEmpty()) {
            name = layer.getName();
        }
        return name;
    }

    private void logException(String prefix, Exception ex) {
        log(prefix + "：" + ex.getMessage());
        ex.printStackTrace();
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static final class TreeItem {
        private final Datasource datasource;
        private final Dataset dataset;

        private TreeItem(Datasource datasource) {
            this.datasource = datasource;
            this.dataset = null;
        }

        private TreeItem(Dataset dataset) {
            this.datasource = dataset.getDatasource();
            this.dataset = dataset;
        }

        @Override
        public String toString() {
            if (dataset == null) {
                return datasource.getAlias();
            }
            return dataset.getName() + " (" + dataset.getType() + ")";
        }
    }

    private static final class DatasetItem {
        private final DatasetVector dataset;

        private DatasetItem(DatasetVector dataset) {
            this.dataset = dataset;
        }

        @Override
        public String toString() {
            return dataset.getDatasource().getAlias() + "." + dataset.getName();
        }
    }
}
