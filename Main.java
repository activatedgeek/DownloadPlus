import java.io.File;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
		(new File(tempFolderPath)).mkdirs();
	}
	
	private Button addbtn,playPausebtn,stopbtn;
	private TableView<DownloadUnit> table = new TableView<DownloadUnit>();
	private String windowTitle = "Download++";
	private int width = 700, height = 525;

	@SuppressWarnings({ "rawtypes"})
	private TableColumn fileNameCol, sizeCol, statusCol, transferRateCol, progressCol;
	
	private final ObservableList<DownloadUnit> downloadList = FXCollections.observableArrayList();
	
	public static void main(String[] args) {
		launch(args); 
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		initGUI(stage);
		initToolbarHandlers();
		stage.show();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initGUI(Stage stage){
		BorderPane root = new BorderPane();
		stage.setTitle(windowTitle);
		Scene scene = new Scene(root,width,height);
	    stage.setScene(scene);
	    
		final VBox topContainer = new VBox();  //Creates a container to hold all Menu Objects
		MenuBar mainMenu = new MenuBar();   //Creates our main menu to hold our Sub-Menus
		ToolBar toolBar = new ToolBar();  //Creates our tool-bar to hold the buttons
		
		topContainer.getChildren().addAll(mainMenu,toolBar);
		root.setTop(topContainer);
		
		addbtn = new Button();
		playPausebtn = new Button();
		stopbtn = new Button();
		
		/*** Adding menu items ***/
		
		Menu file = new Menu("File"); 							/*** File Menu ***/
		MenuItem stopDownload = new MenuItem("Stop Download");
		MenuItem remove = new MenuItem("Remove");
		file.getItems().addAll(stopDownload,remove);
		
		Menu downloadMenu = new Menu("Downloads");				/*** Downloads Menu ***/
		MenuItem pauseAll = new MenuItem("Pause All");
		MenuItem stopAll = new MenuItem("Stop All");
		MenuItem scheduler = new MenuItem("Scheduler");
		downloadMenu.getItems().addAll(pauseAll,stopAll,scheduler);
		
		Menu help = new Menu("Help");							/*** Help Menu ***/
		MenuItem helpmenu = new MenuItem("Options");
		help.getItems().addAll(helpmenu);
		
		mainMenu.getMenus().addAll(file, downloadMenu, help);
		
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
		
	    statusCol = new TableColumn("Status");
	    statusCol.setPrefWidth(100);
	     
	    transferRateCol = new TableColumn("Transfer Rate");
	    transferRateCol.setPrefWidth(100);
	     
	    progressCol = new TableColumn("Progress");
	    progressCol.setPrefWidth(130);
	     
	    table.getColumns().addAll(fileNameCol, sizeCol,transferRateCol,statusCol,progressCol);
	    
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
		                    //updateUrl(String )
		                    fileNameCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("fileName"));
		                    downloadList.add(new DownloadUnit(addURL.getText()));
		                    (new Downloader(addURL.getText(), saveAs.getText())).start();
		                    popup.close();
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
					System.out.println(selected.getFileName()); 					
				}
				
			}
		});
		
		stopbtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				DownloadUnit selected = (DownloadUnit)table.getSelectionModel().getSelectedItem();
				//function/java file is called that contain the functionality for pause/stop
				if(selected != null) {
					System.out.println(selected.getFileName()); 					
				}
			}
		});
	}
}




