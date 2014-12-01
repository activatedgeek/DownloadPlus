import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javafx.util.Callback;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
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
import javafx.application.Platform;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

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
	private static TableView<DownloadUnit> table = new TableView<DownloadUnit>();
	private String windowTitle = "Download++";
	private int width = 1000, height = 650;

	@SuppressWarnings({"rawtypes"})
	private TableColumn fileNameCol, sizeCol, statusCol, transferRateCol, percentageCol, progressCol, resumeCapCol, downloadedCol;
	
	private static final ObservableList<DownloadUnit> downloadList = FXCollections.observableArrayList();
	private static final HashMap<Long, DownloadUnit> idToDunit = new HashMap<Long, DownloadUnit>();
	private static final HashMap<Long, Downloader> idToDownloader = new HashMap<Long, Downloader>();
	private static volatile long uid = 0;
	
	//private boolean paused = false;
	
	public static SplitPane infoPane = new SplitPane();
	public static GridPane gridPane = new GridPane();
	static Label fileNameLabel = new Label();
	static Label sizeLabel = new Label();
	static Label statusLabel = new Label();
	static Label fileTypeLabel = new Label();
	static Label filePathLabel = new Label();
	
	final static VBox topContainer = new VBox();
	
	public static void main(String[] args) {
		Platform.setImplicitExit(false);
		launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		initGUI(stage);
		initToolbarHandlers();
		if(table != null)
			table.addEventHandler(MouseEvent.MOUSE_CLICKED, new TableClickHandler());
		stage.show();
		
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	          public void handle(WindowEvent we) {
	        	  for(Map.Entry<Long, Downloader> dwnld: idToDownloader.entrySet()){
	        		  dwnld.getValue().pauseDownload(true);
	        	  }
				}
	      });
		loadDownloadState();
	}
	
	private void loadDownloadState(){
		new Thread(){
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public void run(){
				try{
					for(final File file: (new File(JSON.dumpPath)).listFiles()){
						if(!file.isDirectory()){
							String ext = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."), file.getAbsolutePath().length());
							if(ext.equals(".data")){
								DownloadUnit dUnit = JSON.loadDumpDownload(file.getAbsolutePath());
								
								idToDunit.put(dUnit.getUID(), dUnit);
	                    		Downloader dwnld = new Downloader(dUnit);
	                    		idToDownloader.put(dUnit.getUID(), dwnld);
	                    		downloadList.add(dUnit);
			                    if(dUnit.statusEnum != DownloadUnit.Status.COMPLETED){
			                    	dUnit.statusEnum = DownloadUnit.Status.RESUMED;
			                    	dwnld.start();
			                    }
			                    
			                    if(uid>dUnit.getUID())
			                    	uid = dUnit.getUID();
			                    file.delete();
							}
						}
					}
					uid++;
					
                    fileNameCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("filename"));
                    sizeCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("size"));
                    statusCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("status"));
                    transferRateCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("transferRate"));
                    resumeCapCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("resumeCap"));
                    downloadedCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("downloaded"));
                    percentageCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("percentage"));
                    
                    resumeCapCol.setCellFactory(new Callback<TableColumn, TableCell>() {
                        public TableCell call(TableColumn param) {
                            return new TableCell<DownloadUnit, String>() {

                                @Override
                                public void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (!isEmpty()) {
                                        if(item.contains("Yes"))
                                            this.setTextFill(Color.GREEN);
                                        else if(item.contains("No"))
                                            this.setTextFill(Color.RED);
                                        setText(item);
                                    }
                                    else
                                        setText(null);
                                }
                            };
                        }
                    });
				}catch(Exception e){
					Logger.log(Logger.Status.ERR_LOAD, "Error restoring state: "+e.getMessage());
				}
			}
		}.start();
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
		Image playimage = new Image(getClass().getResourceAsStream("images/add.png"));
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
	    
	    percentageCol = new TableColumn("Percentage");
	    percentageCol.setPrefWidth(120);
	    
	    progressCol = new TableColumn("Progress");
	    progressCol.setPrefWidth(200);
	    progressCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit, Double>("progress"));
	    progressCol.setCellFactory(ProgressBarTableCell.<DownloadUnit> forTableColumn());
		        
	    table.getColumns().addAll(fileNameCol, sizeCol, downloadedCol, percentageCol, progressCol, transferRateCol, resumeCapCol, statusCol);
        
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
				popup.setResizable(false);
				
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
				
				String clipboardContent=null;
				try {
					clipboardContent = (String) Toolkit.getDefaultToolkit()
					        .getSystemClipboard().getData(DataFlavor.stringFlavor);	 
				} catch (Exception e) {
					Logger.log(Logger.Status.ERR_CLIPBOARD, e.getMessage());
				} 
				if (Pattern.compile("((mailto\\:|(news|(ht|f)tp(s?))\\://){1}\\S+)").matcher(clipboardContent).find()) {
					addURL.setText(clipboardContent);
				}
				
				saveAs.setText(System.getProperty("user.home")+File.separator+"Downloads");
				saveAs.setPrefColumnCount(20);
				GridPane.setConstraints(saveAs, 0, 1);
				grid.getChildren().addAll(addURL, saveAs);

		        Button okbtn = new Button("OK");
		        okbtn.setPrefWidth(100);
		        GridPane.setConstraints(okbtn, 1, 0);

		        Button cancelbtn = new Button("Cancel");
		        cancelbtn.setPrefWidth(100);
		        cancelbtn.setCancelButton(true);
		        GridPane.setConstraints(cancelbtn, 1, 1);
		        
		        Button browsebtn = new Button("Browse");
		        browsebtn.setPrefWidth(100);
		        GridPane.setConstraints(browsebtn, 0, 2);
		  
		        final Label label = new Label();
		        GridPane.setConstraints(label, 0, 3);
		        GridPane.setColumnSpan(label, 1);
		        
		        grid.getChildren().addAll(okbtn,cancelbtn,browsebtn,label);
		        
		        // event when "OK" is clicked
		        okbtn.setOnAction(new EventHandler<ActionEvent>() {
		            @SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
		            public void handle(ActionEvent e) {
		            	Boolean URLvalid = (Pattern.compile("((mailto\\:|(news|(ht|f)tp(s?))\\://){1}\\S+)").matcher(addURL.getText()).find());
						
		                if ((addURL.getText() != null && !addURL.getText().isEmpty()) && URLvalid){
		                    popup.close();
		                    String[] batchList = (addURL.getText().split("\n"));
		                    for(int i=0; i<batchList.length; ++i){
		                    	final int index = i;
			                    new Thread(){
			                    	public void run(){
			                    		uid++;
			                    		DownloadUnit dUnit = new DownloadUnit(batchList[index]);
			                    		dUnit.setProperty(DownloadUnit.TableField.FOLDER, (String)saveAs.getText());
			                    		dUnit.setUID(uid);
			                    		idToDunit.put(uid, dUnit);
			                    		Downloader dwnld = new Downloader(dUnit);
			                    		Logger.debug(dUnit.getUID()+"");
			                    		idToDownloader.put(uid, dwnld);
			                    		
			                    		downloadList.add(dUnit);
					                    dwnld.start();
			                    	}
			                    }.start();
		                    }
		                    
		                    fileNameCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("filename"));
		                    sizeCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("size"));
		                    statusCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("status"));
		                    transferRateCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("transferRate"));
		                    resumeCapCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("resumeCap"));
		                    downloadedCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("downloaded"));
		                    percentageCol.setCellValueFactory(new PropertyValueFactory<DownloadUnit,String>("percentage"));
		                    
		                    resumeCapCol.setCellFactory(new Callback<TableColumn, TableCell>() {
                                public TableCell call(TableColumn param) {
                                    return new TableCell<DownloadUnit, String>() {

                                        @Override
                                        public void updateItem(String item, boolean empty) {
                                            super.updateItem(item, empty);
                                            if (!isEmpty()) {
                                                if(item.contains("Yes"))
                                                    this.setTextFill(Color.GREEN);
                                                else if(item.contains("No"))
                                                    this.setTextFill(Color.RED);
                                                setText(item);
                                            }
                                            else
                                                setText(null);
                                        }
                                    };
                                }
                            });
		                } 
		                else
		                	label.setText("Please enter a URL");
		            }
		        });
		        
		        browsebtn.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						DirectoryChooser dc = new DirectoryChooser();
						dc.setTitle("Browse Directory");
						File file = dc.showDialog(null);
						if(file!=null)
							saveAs.setText(file.getPath());
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
					
					if (selected.getProperty(DownloadUnit.TableField.STATUS)=="Downloading") {
						//change button images
						Image playimage = new Image(getClass().getResourceAsStream("images/play.png"));
						playPausebtn.setGraphic(new ImageView(playimage));
						idToDownloader.get(selected.getUID()).pauseDownload();
					}
					else if (selected.getProperty(DownloadUnit.TableField.STATUS)=="Paused") {
						idToDownloader.remove(selected.getUID());
						
						selected.statusEnum = DownloadUnit.Status.RESUMED;
                		Downloader dwnld = new Downloader(selected);
                		idToDownloader.put(selected.getUID(), dwnld);
                		dwnld.start();
                		
						selected.setProperty(DownloadUnit.TableField.STATUS,"Downloading");
						//change button image
						Image pauseimage = new Image(getClass().getResourceAsStream("images/pause.png"));
						playPausebtn.setGraphic(new ImageView(pauseimage));
					}
					
				}
			}
		});
		
		stopbtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				DownloadUnit selected = (DownloadUnit)table.getSelectionModel().getSelectedItem();
				fileNameLabel.setText("");
				sizeLabel.setText("");
				statusLabel.setText("");
				fileTypeLabel.setText("");
				filePathLabel.setText("");
				
				if(selected != null) {
					infoPane.getItems().removeAll(gridPane);
					gridPane.getChildren().removeAll(fileNameLabel,sizeLabel,statusLabel,fileTypeLabel,filePathLabel);
					topContainer.getChildren().remove(infoPane);
					
					/* destroy object and related temporary dependencies */
					Downloader removed = idToDownloader.get(selected.getUID());
					removed.destroyDownload();
					
					idToDownloader.remove(selected.getUID());
					idToDunit.remove(selected.getUID());
					downloadList.remove(selected);
				}
			}
		});
	}
	
	class TableClickHandler implements EventHandler<MouseEvent>{

		@SuppressWarnings("rawtypes")
		@Override
		public void handle(MouseEvent event) {
			TableView tab = (TableView)event.getSource();
			DownloadUnit selected = (DownloadUnit)tab.getSelectionModel().getSelectedItem();
			
				if (selected != null && event.getClickCount()>1 && selected.getProperty(DownloadUnit.TableField.STATUS)=="Completed") {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().open(new File((String)selected.getProperty(DownloadUnit.TableField.FOLDER)));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				else if (event.getClickCount() == 1 && selected != null){
					if (selected.getProperty(DownloadUnit.TableField.STATUS)=="Downloading") {
						Image pauseimage = new Image(getClass().getResourceAsStream("images/pause.png"));
						playPausebtn.setGraphic(new ImageView(pauseimage));
					}
					
					else if (selected.getProperty(DownloadUnit.TableField.STATUS)=="Paused") {
						Image playimage = new Image(getClass().getResourceAsStream("images/play.png"));
						playPausebtn.setGraphic(new ImageView(playimage));
					}
					
					infoPane.getItems().removeAll(gridPane);
					gridPane.getChildren().removeAll(fileNameLabel, sizeLabel, statusLabel, fileTypeLabel, filePathLabel);
					topContainer.getChildren().remove(infoPane);
					
					if(selected!=null){
						fileNameLabel.setText("File Name: "+(String)selected.getProperty(DownloadUnit.TableField.FILENAME));
		            	sizeLabel.setText("File Size: "+(String)selected.getProperty(DownloadUnit.TableField.SIZE));
		            	statusLabel.setText("Status: "+(String)selected.getProperty(DownloadUnit.TableField.STATUS));
		            	fileTypeLabel.setText("File Type: "+(String)selected.getProperty(DownloadUnit.TableField.TYPE));
		            	filePathLabel.setText("File Location: "+(String)selected.getProperty(DownloadUnit.TableField.FOLDER));
					}
					
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
}

