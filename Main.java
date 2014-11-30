import java.io.File;
import java.util.HashMap;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Main extends Application {
	public static String tempFolderPath = System.getProperty("user.home")+File.separator + ".downloadPlusPlus" + File.separator + "segments";
	
	static{
		Logger.enableDebug();
		Logger.enableLog();
		File toTemp = new File(tempFolderPath);
		if(!toTemp.isDirectory())
			toTemp.mkdirs();
	}
	
	private Button addbtn,playPausebtn,stopbtn;
	private TableView<DownloadUnit> table = new TableView<DownloadUnit>();
	private String windowTitle = "Download++";
	private int width = 800, height = 650;

	@SuppressWarnings({"rawtypes"})
	private TableColumn fileNameCol, sizeCol, statusCol, transferRateCol, progressCol, resumeCapCol, downloadedCol;
	
	private static final ObservableList<DownloadUnit> downloadList = FXCollections.observableArrayList();
	private static final HashMap<Long, DownloadUnit> idToDunit = new HashMap<Long, DownloadUnit>();
	private static final HashMap<Long, Downloader> idToDownloader = new HashMap<Long, Downloader>();
	private static long uid = 0;
	
	private boolean paused = false;
	
	public SplitPane infoPane = new SplitPane();
	public GridPane gridPane = new GridPane();
	Label fileNameLabel = new Label();
	Label sizeLabel = new Label();
	Label statusLabel = new Label();
	Label fileTypeLabel = new Label();
	Label filePathLabel = new Label();
	
	final VBox topContainer = new VBox();
	
	public static void main(String[] args) {
		//JSON.loadDumps(System.getProperty("user.home")+File.separator+"Downloads");
		launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		initGUI(stage);
		initToolbarHandlers();
		table.addEventHandler(MouseEvent.MOUSE_CLICKED, new TableClickHandler());
		stage.show();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initGUI(Stage stage){
		BorderPane root = new BorderPane();
		stage.setTitle(windowTitle);
		Scene scene = new Scene(root,width,height);
		stage.setMinWidth(width);
		stage.setMinHeight(height);
	    stage.setScene(scene);
	    stage.sizeToScene();
	    
		ToolBar toolBar = new ToolBar();  //Creates our tool-bar to hold the buttons
		
		topContainer.getChildren().addAll(toolBar);
		root.setTop(topContainer);
		
		addbtn = new Button();
		playPausebtn = new Button();
		stopbtn = new Button();
		
		/*** Toolbar ***/
		Image playimage = new Image(getClass().getResourceAsStream("images/play.png"));
		addbtn.setGraphic(new ImageView(playimage));
		
		Image pauseimage = new Image(getClass().getResourceAsStream("images/pause.png"));
		playPausebtn.setGraphic(new ImageView(pauseimage));
		
		Image stopimage = new Image(getClass().getResourceAsStream("images/stop.png"));
		stopbtn.setGraphic(new ImageView(stopimage));
		
		toolBar.getItems().addAll(addbtn,playPausebtn,stopbtn);
		toolBar.getStylesheets().add("css/downloadboxStyle.css");
		
		//list of downloads
		table.setEditable(true);
		
		/*** Information Table ***/
		fileNameCol = new TableColumn("File Name");
		fileNameCol.setPrefWidth(120);
	     
		sizeCol = new TableColumn("Size");
		sizeCol.setPrefWidth(100);
		
		resumeCapCol = new TableColumn("Resumable");
		resumeCapCol.setPrefWidth(100);
		
	    statusCol = new TableColumn("Status");
	    statusCol.setPrefWidth(100);
	     
	    transferRateCol = new TableColumn("Transfer Rate");
	    transferRateCol.setPrefWidth(100);
	    
	    downloadedCol = new TableColumn("Downloaded");
	    downloadedCol.setPrefWidth(120);
	    		
	    progressCol = new TableColumn("Progress");
	    progressCol.setPrefWidth(130);
	    progressCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit, Double>("progress"));
	    progressCol.setCellFactory(ProgressBarTableCell.<DownloadUnit> forTableColumn());
        
	    table.getColumns().addAll(fileNameCol, sizeCol, resumeCapCol, statusCol, transferRateCol, downloadedCol, progressCol);
        
	    table.setItems(downloadList);
	    topContainer.getChildren().addAll(table);
        
	    stage.setScene(scene);
	}
	
	private void initToolbarHandlers(){
		addbtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				final Stage popup = new Stage();
				popup.initModality(Modality.APPLICATION_MODAL);
				popup.setTitle("Enter URL(s) to download");
				
				BorderPane popupRoot = new BorderPane();
				
				Scene scene = new Scene(popupRoot,650,200);
			    popup.setScene(scene);
			    
			    GridPane grid = new GridPane();
		        grid.setPadding(new Insets(10, 10, 10, 10));
		        grid.setVgap(5);
		        grid.setHgap(5);
		        
		        scene.setRoot(grid);
		        
		        final TextArea addURL = new TextArea();
		        final TextField saveAs = new TextField();
		        
		        addURL.setPromptText("Add Url");
				addURL.setPrefColumnCount(45);
				addURL.setPrefHeight(175);
				GridPane.setConstraints(addURL, 0, 0);
				
				saveAs.setText(System.getProperty("user.home")+File.separator+"Downloads");
				saveAs.setPrefColumnCount(20);
				GridPane.setConstraints(saveAs, 0, 1);
				//GridPane.setColumnSpan(saveAs, 0);
				grid.getChildren().addAll(addURL, saveAs);

		        Button okbtn = new Button("OK");
		        GridPane.setConstraints(okbtn, 1, 0);

		        Button cancelbtn = new Button("Cancel");
		        GridPane.setConstraints(cancelbtn, 1, 1);
		  
		        final Label label = new Label();
		        GridPane.setConstraints(label, 0, 2);
		        GridPane.setColumnSpan(label, 1);
		        
		        grid.getChildren().addAll(okbtn,cancelbtn,label);
		        
		        // event when "OK" is clicked
		        okbtn.setOnAction(new EventHandler<ActionEvent>() {
		            @SuppressWarnings("unchecked")
					@Override
		            public void handle(ActionEvent e) {
		                if ((addURL.getText() != null && !addURL.getText().isEmpty())){
		                    popup.close();
		                    
		                    new Thread(){
		                    	public void run(){
		                    		DownloadUnit dUnit = new DownloadUnit(addURL.getText());
		                    		dUnit.setProperty(DownloadUnit.TableField.FOLDER, (String)saveAs.getText());
		                    		dUnit.setUID(uid);
		                    		idToDunit.put(uid, dUnit);
		                    		Downloader dwnld = new Downloader(dUnit);
		                    		idToDownloader.put(uid++, dwnld);
		                    		
		                    		downloadList.add(dUnit);
				                    dwnld.start();
		                    	}
		                    }.start();
		                    
		                    fileNameCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("filename"));
		                    sizeCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("size"));
		                    statusCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("status"));
		                    transferRateCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("transferRate"));
		                } 
		                else {
		                	label.setText("Please enter a URL");
		                }
		            }
		        });
		        
		        cancelbtn.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						popup.close();
					}
				});
				popup.show();
		}
		});
		
		playPausebtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				DownloadUnit selected = (DownloadUnit)table.getSelectionModel().getSelectedItem();
				//function/java file is called that contain the functionality for pause/stop
				if(selected != null) {
					if(!paused){
						idToDownloader.get(selected.getUID()).pauseDownload();
						paused = true;
					}
				}
				
			}
		});
		
		stopbtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				DownloadUnit selected = (DownloadUnit)table.getSelectionModel().getSelectedItem();
				//function/java file is called that contain the functionality for pause/stop
				if(selected != null) {
					downloadList.remove(selected);
					
					infoPane.getItems().removeAll(gridPane);
					gridPane.getChildren().removeAll(fileNameLabel,sizeLabel,statusLabel,fileTypeLabel,filePathLabel);
					topContainer.getChildren().remove(infoPane);
				}
			}
		});
	}
	
	class TableClickHandler implements EventHandler<MouseEvent>{

		@Override
		public void handle(MouseEvent event) {
			TableView tab = (TableView)event.getSource();
			DownloadUnit selected = (DownloadUnit)tab.getSelectionModel().getSelectedItem();
			
			infoPane.getItems().removeAll(gridPane);
			gridPane.getChildren().removeAll(fileNameLabel, sizeLabel, statusLabel, fileTypeLabel, filePathLabel);
			topContainer.getChildren().remove(infoPane);
			
			fileNameLabel.setText("File Name: "+(String)selected.getProperty(DownloadUnit.TableField.FILENAME));
            sizeLabel.setText("File Size: "+(String)selected.getProperty(DownloadUnit.TableField.SIZE));
            statusLabel.setText("Status: "+(String)selected.getProperty(DownloadUnit.TableField.STATUS));
            fileTypeLabel.setText("File Type: "+(String)selected.getProperty(DownloadUnit.TableField.FILENAME));
            filePathLabel.setText("File Location: "+(String)selected.getProperty(DownloadUnit.TableField.FOLDER));
           
            GridPane.setConstraints(fileNameLabel, 0, 0);
            GridPane.setConstraints(sizeLabel, 0, 1);
            GridPane.setConstraints(statusLabel, 0, 2);
            GridPane.setConstraints(fileTypeLabel, 0, 3);
            GridPane.setConstraints(filePathLabel, 0, 4);

            gridPane.getChildren().addAll(fileNameLabel, sizeLabel, statusLabel, fileTypeLabel,filePathLabel);                
            infoPane.getItems().add(gridPane);
            topContainer.getChildren().add(infoPane);
		}
		
	}
}
