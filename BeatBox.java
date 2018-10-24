import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class BeatBox{
  JFrame frame;
  JPanel mainPanel;
  DefaultListModel model = new DefaultListModel();
  JList nameList = new JList(model);
  JTextField messageToSend;
  ArrayList<JCheckBox> checkBoxList;
  Sequencer sequencer;
  Sequence sequence;
  Track track;
  //IO
  ObjectInputStream in;
  ObjectOutputStream out;
  String userName;
  //hashMap
  HashMap<String, boolean[]> sequenceMap = new HashMap<String, boolean[]>();


  String[] instrumentsName = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand CLap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
  int[] instruments = {35, 42, 46, 38, 49,39,50,60,70,72,64,56,58,47,67,63};

  public static void main(String[] args){
    BeatBox beatBox = new BeatBox();
    beatBox.setUp(args[0]);
  }

  public void setUp(String userName){
    this.userName = userName;
    try{
      Socket sock = new Socket("127.0.0.1", 4242);
      out = new ObjectOutputStream(sock.getOutputStream());
      in = new ObjectInputStream(sock.getInputStream());
      Thread thread1 = new Thread(new GetComingMessage());
      thread1.start();
    }catch(Exception e){
      System.out.println("Fail to connect with server");
    }
    buildGUI();
  }

  public void buildGUI(){
    frame = new JFrame("Cyber Beatbox");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    BorderLayout borderLayout = new BorderLayout();
    JPanel background = new JPanel(borderLayout);
    background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    checkBoxList = new ArrayList<JCheckBox>();

    Box buttonBox = new Box(BoxLayout.Y_AXIS);
    Box nameBox = new Box(BoxLayout.Y_AXIS);

    for(int i = 0; i< 16; i++){
      nameBox.add(new Label(instrumentsName[i]));
    }

    JButton start = new JButton("Start");
    start.addActionListener(new StartListener());
    buttonBox.add(start);

    JButton stop = new JButton("Stop");
    stop.addActionListener(new StopListener());
    buttonBox.add(stop);

    JButton tempoUp = new JButton("Temp up");
    tempoUp.addActionListener(new UpTempListener());
    buttonBox.add(tempoUp);

    JButton tempDown = new JButton("Temp down");
    tempDown.addActionListener(new DownTempListener());
    buttonBox.add(tempDown);

    JButton clearAll =  new JButton("Clear all");
    clearAll.addActionListener(new ClearAllListener());
    buttonBox.add(clearAll);

    JButton store = new JButton("Store");
    store.addActionListener(new StoreListener());
    buttonBox.add(store);

    JButton restore = new JButton("Restore");
    restore.addActionListener(new RestoreListener());
    buttonBox.add(restore);

    JButton sendIt = new JButton("Send it");
    sendIt.addActionListener(new SendItListener());
    buttonBox.add(sendIt);

    background.add(BorderLayout.WEST, nameBox);
    background.add(BorderLayout.EAST, buttonBox);

    //Add JList and JTextField to the background
    messageToSend = new JTextField();
    //JScrollPane theMessageToSend = new JScrollPane(nameList);
    buttonBox.add(messageToSend);

    nameList.addListSelectionListener(new SelectionListener());
    nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane theList = new JScrollPane(nameList);
    buttonBox.add(theList);

    GridLayout grid = new GridLayout(16,16);

    mainPanel = new JPanel(grid);
    background.add(BorderLayout.CENTER, mainPanel);
    frame.getContentPane().add(background);

    for(int i = 0; i < 256; i++){
      JCheckBox cb = new JCheckBox();
      cb.setSelected(false);
      checkBoxList.add(cb);
      mainPanel.add(cb);
    }

    setupMiDi();

    frame.setBounds(50,50,300,300);
    frame.pack();
    frame.setVisible(true);
    }

    public void setupMiDi(){
      try{
        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequence = new Sequence(Sequence.PPQ, 4);
        track = sequence.createTrack();
        sequencer.setTempoInBPM(120);
      }catch(Exception e){
        e.printStackTrace();
      }
    }

    public void buildTrackAndStart(){
      int[] trackList = new int[16];

      sequence.deleteTrack(track);
      track = sequence.createTrack();

      for(int i = 0; i<16; i++){
        int key = instruments[i];
        for(int j = 0; j<16; j++){
          JCheckBox cb = checkBoxList.get(j+i*16);
          if(cb.isSelected()){
            trackList[j] = key;
          }else{
            trackList[j] = 0;
          }
        }
        makeTracks(trackList);
        track.add(makeEvent(176,1,127,0,16));
      }
    track.add(makeEvent(192,9,1,0,15));
    try{
      sequencer.setSequence(sequence);
      sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
      sequencer.start();
      sequencer.setTempoInBPM(120);
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public class StartListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      buildTrackAndStart();
    }
  }

  public class StopListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      sequencer.stop();
    }
  }

  public class UpTempListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float)(tempoFactor * 1.03));

    }
  }

  public class DownTempListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float)(tempoFactor * .97));
    }
  }

  public class ClearAllListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      for(int i = 0; i<256; i++){
        checkBoxList.get(i).setSelected(false);
      }
    }
  }

  public class StoreListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      boolean[] checkBoxState = new boolean[256];
      for(int i = 0; i<256; i++){
        if(checkBoxList.get(i).isSelected()==true){
          checkBoxState[i] = true;
        }else{
          checkBoxState[i] = false;
        }
      }
      try{
        FileOutputStream fileStream = new FileOutputStream("checkBoxState.ser");
        ObjectOutputStream os = new ObjectOutputStream(fileStream);
        os.writeObject(checkBoxState);
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
  }

  public class RestoreListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      boolean[] checkBoxState = null;
      try{
        FileInputStream fileStream = new FileInputStream("checkBoxState.ser");
        ObjectInputStream is = new ObjectInputStream(fileStream);
        checkBoxState = (boolean[]) is.readObject();
      }catch(Exception ex){
        ex.printStackTrace();
      }
      for(int i = 0; i < 256; i++){
        if(checkBoxState[i] == true){
          checkBoxList.get(i).setSelected(true);
        }else{
          checkBoxList.get(i).setSelected(false);
        }
      }
    }
  }

  public class SendItListener implements ActionListener{
    public void actionPerformed(ActionEvent a){
      boolean[] checkBoxState = new boolean[256];
      for(int i = 0; i<256; i++){
        if(checkBoxList.get(i).isSelected()==true){
          checkBoxState[i] = true;
        }else{
          checkBoxState[i] = false;
        }
      }
      try{
        out.writeObject(userName + ": " + messageToSend.getText());
        out.writeObject(checkBoxState);
      }catch(Exception ex){
        ex.printStackTrace();
      }
      messageToSend.setText("");
    }
  }

  public class SelectionListener implements ListSelectionListener{
    public void valueChanged(ListSelectionEvent le){
      if(!le.getValueIsAdjusting()){
        String selected = (String) nameList.getSelectedValue();
        boolean[] newSequence = (boolean[]) sequenceMap.get(selected);
        sequencer.stop();
        for(int i = 0; i < 256; i++){
          if(newSequence[i] == true){
            checkBoxList.get(i).setSelected(true);
          }else{
            checkBoxList.get(i).setSelected(false);
          }
        }
      }
    }
  }

  public class GetComingMessage implements Runnable{
    public void run(){
      String name;
      boolean[] recievedSequence =null;
      try{
        while((name = (String) in.readObject()) != null){
          recievedSequence = (boolean[]) in.readObject();
          sequenceMap.put(name, recievedSequence);
          int pos = nameList.getModel().getSize();
          model.add(pos, name);
        }
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
  }

  public void makeTracks(int[] list){
    for(int i = 0; i < 16; i++){
      int key = list[i];
      if(key != 0){
        track.add(makeEvent(144,9,key,100,i));
        track.add(makeEvent(128,9,key,100,i+1));
      }
    }
  }

  public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
    MidiEvent event = null;
    try{
      ShortMessage a = new ShortMessage();
      a.setMessage(comd, chan, one, two);
      event = new MidiEvent(a, tick);
    }catch(Exception e){
      e.printStackTrace();
    }
    return event;
  }
}
