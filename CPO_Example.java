import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.io.*;

/**
 
 0 = vacant
 1 = CH4
 2 = CH3
 3 = Ch2
 4 = CH
 5 = H
 6 = C
 7 = O
 8 = Ox
 9 = CO
 10 = OH

 **/

class CPO extends Canvas implements Runnable {
	
	int size = 256/2;								// number of lattice sites in a row (change if desired)
	int squareWidth = 3;						// pixels across one lattice site (change if desired)
	int canvasSize = size * squareWidth;		// total pixels across canvas
    int[][] s = new int[size][size];   			// the 2D array of surface sites
    int[] il = new int[4];
    int[] jl = new int[4];
	boolean running = false;					// true when simulation is running
    boolean refresh = true;
	Button startButton = new Button(" Start ");
    Button resetButton = new Button(" Reset ");
	Scrollbar tScroller;						// scrollbar to adjust pco
	Scrollbar dScroller;						// scrollbar to adjust desorption
	Scrollbar iScroller;						// scrollbar to adjust inerts
	Label tLabel = new Label("");	// text label next to scrollbar
	Label dLabel = new Label("");	// text label next to scrollbar
	Label iLabel = new Label("");	// text label next to scrollbar
	DecimalFormat threePlaces = new DecimalFormat("0.000");	// to format pco readout
	Image offScreenImage;						// for double-buffering
	Graphics offScreenGraphics;
    Color oxygenColor = new Color(255,0,0);	    // red
    Color coColor = new Color(0,0,255);	       // blue
    Color inertColor = new Color(0,0,0);	       // black
	Color emptyColor = new Color(255,255,255);	
	Color oneColor = new Color(255,0,0);
	Color twoColor = new Color(128,0,0);
	Color threeColor = new Color(128,128,0);
	Color fourColor = new Color(0,128,128);
	Color fiveColor = new Color(0,128,0);
	Color sixColor = new Color(0,255,0);
	Color sevenColor = new Color(0,0,255);
	Color eightColor = new Color(255,255,0);
	Color nineColor = new Color(0,255,255);
	Color tenColor = new Color(255,0,255);
					double ad1 = 0;
				double ad7 = 0;
	
	
	
	// Constructor method handles all the initializations:
	CPO() {
		Frame zgbFrame = new Frame("CPO Model");	// initialize the GUI...
		zgbFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);							// close button exits program
			}
		});
		Panel canvasPanel = new Panel();
		zgbFrame.add(canvasPanel);
		canvasPanel.add(this);
		setSize(canvasSize,canvasSize);
		Panel controlPanel = new Panel();
		zgbFrame.add(controlPanel,BorderLayout.SOUTH);
		controlPanel.add(tLabel);
		tScroller = new Scrollbar(Scrollbar.HORIZONTAL,500,1,000,601) {
			public Dimension getPreferredSize() {
				return new Dimension(100,15);			// make it bigger than default
			}
		};
		tScroller.setBlockIncrement(1);		// enables fine adjustments in Mac OS X 10.7+
		tScroller.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				tLabel.setText("Pco=" + threePlaces.format(tScroller.getValue()/1000.0));
			}
		});
		controlPanel.add(tScroller);
        
        controlPanel.add(dLabel);
        dScroller = new Scrollbar(Scrollbar.HORIZONTAL,0,1,0,101) {
			public Dimension getPreferredSize() {
				return new Dimension(100,15);			// make it bigger than default
			}
		};
		dScroller.setBlockIncrement(1);		// enables fine adjustments in Mac OS X 10.7+
		dScroller.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				dLabel.setText("Pdes=" + threePlaces.format(dScroller.getValue()/1000.0));
			}
		});
		controlPanel.add(dScroller);

        controlPanel.add(iLabel);
        iScroller = new Scrollbar(Scrollbar.HORIZONTAL,0,1,0,101) {
			public Dimension getPreferredSize() {
				return new Dimension(100,15);			// make it bigger than default
			}
		};
		iScroller.setBlockIncrement(1);		// enables fine adjustments in Mac OS X 10.7+
		iScroller.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				iLabel.setText("" + threePlaces.format(iScroller.getValue()/1000.0));
			}
		});
		controlPanel.add(iScroller);

        
		controlPanel.add(new Label("     "));			// leave some space
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = !running;
				if (running) startButton.setLabel("Pause"); else startButton.setLabel("Resume");
			}
		});
		controlPanel.add(startButton);
        
        controlPanel.add(new Label("     "));			// leave some space
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                refresh = true;
                if (running == false)
                {    for (int i=0; i<size; i++) 					// initialize the lattice...
                    for (int j=0; j<size; j++) {
                        s[i][j] = 0;
                        colorSquare(i,j);
                    }
                repaint();
                }
            
                //resetButton.setLabel("Reset");
				//if (running) resetButton.setLabel("Reset"); else resetButton.setLabel("Resume");
			}
		});
		controlPanel.add(resetButton);
        
        
		zgbFrame.pack();
		offScreenImage = createImage(canvasSize,canvasSize);
		offScreenGraphics = offScreenImage.getGraphics();

		/*for (int i=0; i<size; i++)
        {					// initialize the lattice...
			for (int j=0; j<size; j++)
            {	//if (Math.random() < 0.1) s[i][j] = 1; else
                s[i][j] = 0;
                colorSquare(i,j);
            }
        }
		*/

		zgbFrame.setVisible(true);		// we're finally ready to show it!

		Thread t = new Thread(this);		// create a thread to run the simulation
		t.start();							// and let 'er rip...
	}

	// Run method gets called by new thread to carry out the simulation:
	public void run() {
	
	// Define N : number of time step
	double N=0;
	int L=size;
	double rxH2 = 0;
	double rxH2O = 0;
	double rxCO = 0;
	double rxCO2 = 0;
	double RxH2 = 0;
	double RxH2O = 0;
	double RxCO = 0;
	double RxCO2 = 0;
					double Zx0=0;
					double Zx1=0;
					double Zx2=0;
					double Zx3=0;
					double Zx4=0;
					double Zx5=0;
					double Zx6=0;
					double Zx7=0;
					double Zx8=0;
					double Zx9=0;
					double Zx10=0;

		while ((true) & (N<51)) {
			if (running) {
			
				double pco = tScroller.getValue() / 1000.0;
                double pdes = dScroller.getValue() / 1000.0;
                double pinert = iScroller.getValue() / 1000.0;
                int ip, jp;
				
				// Operating condition
				double T = 873;
				/*
				double P1 = (6.72E-05)*3.6*(1E+04);
				double P7 = 0.0011*1.67*(1E+04);	
				*/
				double x=0.67;
				double P = 6*(1E+03);
				double P1 = (6E-05)*x*P;
				double P7 = 0.0011*(1-x)*P;
				
				
				
				// Pre-exponential factor
				double A1 = 75;
				double A2 = 1E+04;
				double A7 = 10;
				double A8 = 1E+11;
				double A9 = 1E+12;
				double A11 =1E+05; //note
				double A12 = 1E+07;
				double A13 = 1E+10;
				double A14 = 5E+06;
				double A15 = 1E+05;
				double A16 = 1E+07;
				double A17 = 5.2E+03;
				double AD = 5.45E-07;
				
				// Activation Energy
				double E2 = 7.9;
				double E8 = 44.6;
				double E9 = 35.42;
				double E11 = 15.7; //note
				double E12 = 26.24;
				double E13 = 27.85;
				double E14 = 15.2;
				double E15 = 17;
				double E16 = 19.6;
				double E17 = 11;
				double ED = 158.65;
				
				double a = 2.48E-08;
				// Rate constant
				double k1 = A1*P1;
				double k2 = A2*Math.exp(-E2*1000/(1.987*T));
				double k7 = A7*P7;
				double k8 = A8*Math.exp(-E8*1000/(1.987*T));
				double k9= A9*Math.exp(-E9*1000/(1.987*T));
				double k11 = A11*Math.exp(-E11*1000/(1.987*T));
				double k12 = A12*Math.exp(-E12*1000/(1.987*T));
				double k13 = A13*Math.exp(-E13*1000/(1.987*T));
				double k14 = A14*Math.exp(-E14*1000/(1.987*T));
				double k15 = A15*Math.exp(-E15*1000/(1.987*T));
				double k16 = A16*Math.exp(-E16*1000/(1.987*T));
				double k17 = A17*Math.exp(-E17*1000/(1.987*T));
				double k10=0;
				double k18=0;
				double kD = (1E+04)*AD*Math.exp(-ED*1000/(8.314*T))/(a*a);
				
				double DN = kD/k9;
				double D1= Math.floor(DN);
				double D2= DN-D1;
						
				double ktot = k1+k2+k7+k8+k9+k10+k11+k12+k13+k14+k15+k16+k17+k18;
				
				// Probability
				double p1 = k1/ktot;
				double p2 = k2/ktot;
				double p3 = 0;
				double p4 = 0;
				double p5 = 0;
				double p6 = 0;
				double p7 = k7/ktot;
				double p8 = k8/ktot;
				double p9 = k9/ktot;
				double p10 = k10/ktot;
				double p11 = k11/ktot;
				double p12 = k12/ktot;
				double p13 = k13/ktot;
				double p14 = k14/ktot;
				double p15 = k15/ktot;
				double p16 = k16/ktot;
				double p17 = k17/ktot;
				double p18 = k18/ktot;
					
				double ptot=p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14+p15+p16+p17+p18;
				
				//System.out.println("k1 = " + k1);
				//System.out.println("k7 = " + k7);
				
				// Refresh simulation
                if (refresh == true)
                {
                    for (int i=0; i<size; i++) // initialize the lattice...
                        for (int j=0; j<size; j++)
                        {
                          //  if (Math.random() < pinert) s[i][j] = 3; else
                          //  s[i][j] = 0;
                            s[i][j] = 0;
                            colorSquare(i,j);
                        }
                    s[size/2][size/2] = 0;
                    repaint();
                    refresh = false;
                }
				
				// Update time step
				N=N+1;
				
				// Reset production rate
				double RH2=0;
				double RCO=0;
				double RCO2=0;
				double RH2O=0;
				double Z0=0;
				double Z1=0;
				double Z2=0;
				double Z3=0;
				double Z4=0;
				double Z5=0;
				double Z6=0;
				double Z7=0;
				double Z8=0;
				double Z9=0;
				double Z10=0;
				
				for (int step=0; step<((L*L)*1000); step++) {		// adjust number of steps as desired
                    
							
	
							
					
	// Select site
                        int i = (int) (Math.random() * size);	// choose a random row and column
                        int j = (int) (Math.random() * size);
						double p = Math.random();
							
													// When H is selected						
							if (s[i][j] == 5)
                            {
	//step 18a
								if (s[i][j] == 5)
								{
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 10) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 10) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 10) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 10) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
									s[i][j] = 5;
									colorSquare(i,j);
                                }

                                else
                                {
                                    l = (int)(Math.random() * l);
                                    s[i][j] = 0;
                                    colorSquare(i,j);
									s[il[l]][jl[l]] = 0;
                                    colorSquare(il[l],jl[l]);
									RH2O = RH2O+1;
                                }
								}
								
	//Step 10	
								
							}
							
	//When CH4 is selected
                            if (s[i][j] == 1)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 1;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 2;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 2;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }

							
							/* Step3b */
                            
							 if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 1) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 1) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 1) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 1) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 2;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 2;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
							 
							/* Step4a */
                            if (s[i][j] == 2)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 2;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 3;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 3;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }

							
							/* Step4b */
							
                            if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 2) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 2) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 2) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 2) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 3;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 3;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
							
							
							/* Step5a */
                            if (s[i][j] == 3)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 3;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 4;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 4;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }

							/* Step5b */
							
                            if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 3) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 3) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 3) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 3) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 4;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 4;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
								
							/* Step6a*/
                            if (s[i][j] == 4)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 4;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 6;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 6;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }
							 
							/* Step6b */
							
                            if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 4) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 4) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 4) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 4) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 6;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 6;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
							
							
						
					
	// When OH is selected
                            if (s[i][j] == 10)
                            {
							    if (s[i][j] == 10)
								{
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 5) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 5) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 5) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 5) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 10;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
                                        s[i][j] = 0;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 0;
                                        colorSquare(il[l],jl[l]);
										RH2O=RH2O+1;
                                    }
								}
								
	
							}


	//Step 10	
								if (s[i][j] == 5)
								{
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 5) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 5) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 5) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 5) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 5;
                                    colorSquare(i,j);
                                }
                                else
                                {
                                   l = (int)(Math.random() * l);
                                    s[i][j] = 0;
                                    colorSquare(i,j);
									s[il[l]][jl[l]] = 0;
                                    colorSquare(il[l],jl[l]);
									RH2 = RH2+1;
									
								}
								}
								
						/* Random */
                        if (p <= p1)
                        {
                            if (s[i][j] == 0)
                            {
                                s[i][j] = 1;
                                colorSquare(i,j);
								ad1=ad1+1;
                            }
                        }
						else if ((p <= p1+p2) & (p > p1))
						{
							if (s[i][j] == 1)
                            {
                                s[i][j] = 0;
                                colorSquare(i,j);
                            }
						}

						else if ((p <= p1+p2+p3+p4+p5+p6+p7) & (p > p1+p2+p3+p4+p5+p6))
						{
							if (s[i][j] == 0)
                            {
                                {
                               								double r = Math.random();
                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 7;
									colorSquare(i,j);
									s[ip][jp] = 7;
									colorSquare(ip,jp);
									ad7=ad7+1;

                                }
                            }
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8) & (p > p1+p2+p3+p4+p5+p6+p7))
						{
							if (s[i][j] == 7)
                            {
                                {
                                								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 7)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
                                }
                            }
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9) & (p > p1+p2+p3+p4+p5+p6+p7+p8))
						{
						
							if (s[i][j] == 6)
                            {
                                {
                               								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 7)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 9;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 9;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
							if (s[i][j] == 7)
                            {
                                {
                               								double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 6)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 9;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 9;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
						}
						
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10))
						{
							if (s[i][j] == 7)
                            {
                                s[i][j] = 8;
                                colorSquare(i,j);
								

                            }
						}
						
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11))
						{
							if (s[i][j] == 6)
                            {
                                {
                                								double r = Math.random();
                                                              if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 8)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 9;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 9;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
							if (s[i][j] == 8)
                            {
                                {
                               								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 6)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 9;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 9;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12))
						{
							if (s[i][j] == 9)
                            {
                                s[i][j] = 0;
                                colorSquare(i,j);
								RCO = RCO+1;
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13))
						{
							if (s[i][j] == 9)
                            {
                                {
                              								double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 7)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									RCO2 = RCO2+1;
                                }
                            }
                            }
							if (s[i][j] == 7)
                            {
                                {
  								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 9)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									RCO2 = RCO2+1;
                                }
                            }
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14+p15) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14))
						{
							if (s[i][j] == 9)
                            {
                                {
          								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 8)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									RCO2 = RCO2+1;
                                }
                            }
                            }
							if (s[i][j] == 8)
                            {
                                {
                              								double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 9)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									RCO2 = RCO2+1;
                                }
                            }
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14+p15+p16) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14+p15))
						{
							if (s[i][j] == 5)
                            {
                                {
                            								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 7)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 10;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 10;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
							if (s[i][j] == 7)
                            {
                                {
                               								double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 5)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 10;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 10;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
						}
						else if ((p <= p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14+p15+p16+p17) & (p > p1+p2+p3+p4+p5+p6+p7+p8+p9+p10+p11+p12+p13+p14+p15+p16))
						{
							if (s[i][j] == 5)
                            {
                                {
                               								double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 8)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 10;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 10;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
							if (s[i][j] == 8)
                            {
                                {
                            								double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                                if (s[ip][jp] == 5)
                                {
									if (Math.random() < 0.5)
									{
                                    s[i][j] = 10;
									colorSquare(i,j);
									s[ip][jp] = 0;
									colorSquare(ip,jp);
									}
									else
									{
									s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 10;
									colorSquare(ip,jp);
									}
                                }
                            }
                            }
						}
						
							// When H is selected						
							if (s[i][j] == 5)
                            {
	//step 18a
								if (s[i][j] == 5)
								{
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 10) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 10) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 10) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 10) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
									s[i][j] = 5;
									colorSquare(i,j);
                                }

                                else
                                {
                                    l = (int)(Math.random() * l);
                                    s[i][j] = 0;
                                    colorSquare(i,j);
									s[il[l]][jl[l]] = 0;
                                    colorSquare(il[l],jl[l]);
									RH2O = RH2O+1;
                                }
								}
								
	//Step 10	
								
							}
							
	//When CH4 is selected
                            if (s[i][j] == 1)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 1;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 2;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 2;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }

							
							/* Step3b */
                            
							 if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 1) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 1) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 1) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 1) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 2;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 2;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
							 
							/* Step4a */
                            if (s[i][j] == 2)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 2;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 3;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 3;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }

							
							/* Step4b */
							
                            if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 2) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 2) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 2) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 2) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 3;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 3;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
							
							
							/* Step5a */
                            if (s[i][j] == 3)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 3;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 4;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 4;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }

							/* Step5b */
							
                            if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 3) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 3) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 3) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 3) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 4;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 4;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
								
							/* Step6a*/
                            if (s[i][j] == 4)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 0) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 0) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 0) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 0) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 4;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 6;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 6;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }
							 
							/* Step6b */
							
                            if (s[i][j] == 0)
                            {
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 4) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 4) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 4) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 4) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 0;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
										if (Math.random() < 0.5)
										{
                                        s[i][j] = 6;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 5;
                                        colorSquare(il[l],jl[l]);
										}
										else
										{
										s[i][j] = 5;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 6;
                                        colorSquare(il[l],jl[l]);
										}
                                    }
                            }	
							
							
						
					
	// When OH is selected
                            if (s[i][j] == 10)
                            {
							    if (s[i][j] == 10)
								{
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 5) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 5) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 5) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 5) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 10;
                                    colorSquare(i,j);
                                }

                                else
                                    {
                                        l = (int)(Math.random() * l);
                                        s[i][j] = 0;
                                        colorSquare(i,j);
										s[il[l]][jl[l]] = 0;
                                        colorSquare(il[l],jl[l]);
										RH2O=RH2O+1;
                                    }
								}
								
	
							}


	//Step 10	
								if (s[i][j] == 5)
								{
								int l = 0;
                                if (s[(i+1)&(size-1)][j] == 5) {il[l] = (i+1)&(size-1); jl[l] = j; ++l;}
                                if (s[(i-1)&(size-1)][j] == 5) {il[l] = (i-1)&(size-1); jl[l] = j; ++l;}
                                if (s[i][(j+1)&(size-1)] == 5) {il[l] = i; jl[l] = (j+1)&(size-1); ++l;}
                                if (s[i][(j-1)&(size-1)] == 5) {il[l] = i; jl[l] = (j-1)&(size-1); ++l;}
                                if (l == 0)
                                {
                                    s[i][j] = 5;
                                    colorSquare(i,j);
                                }
                                else
                                {
                                   l = (int)(Math.random() * l);
                                    s[i][j] = 0;
                                    colorSquare(i,j);
									s[il[l]][jl[l]] = 0;
                                    colorSquare(il[l],jl[l]);
									RH2 = RH2+1;
									
								}
								}
							

						
                        //colorSquare(i,j);
					
						
							
							
							for (int stepd=0; stepd<=D1; stepd++)  {
							i = (int) (Math.random() * size);
							j = (int) (Math.random() * size);
							if (s[i][j] == 5)
                            {
                                {
                                double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 5;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
									if (s[i][j] == 7)
                            {
                                {
                                double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 7;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
							if (s[i][j] == 1)
                            {
                                {
                                double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 1;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
								
							if (s[i][j] == 9)
                            {
                                {
                                double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 9;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
							}
							
							double pdiff = (Math.random());
							if (pdiff<=DN) {
							i = (int) (Math.random() * size);
							j = (int) (Math.random() * size);
							if (s[i][j] == 5)
                            {
                                {
                                double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 5;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
									if (s[i][j] == 7)
                            {
                                {
                                double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 7;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
							if (s[i][j] == 1)
                            {
                                {
                                double r = Math.random();
                                                                if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 1;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
								
							if (s[i][j] == 9)
                            {
                                {
                                double r = Math.random();
                                                               if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j+1) & (size-1);
                                }
                                else if (r < 0)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j-1) & (size-1);
                                }
								else if (r < 0.25)
                                {
                                    ip = (i+1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.5)
                                {
                                    ip = (i-1) & (size-1);
                                    jp = (j);
                                }
								else if (r < 0.75)
                                {
                                    ip = (i);
                                    jp = (j-1) & (size-1);
                                }
                                else
                                {
                                    ip = (i);
                                    jp = (j+1) & (size-1);
                                }
                            
                                if (s[ip][jp] == 0)
                                {
                                    s[i][j] = 0;
									colorSquare(i,j);
									s[ip][jp] = 9;
									colorSquare(ip,jp);
                                }
                            }
                            }
							
							
							} 
				}
				
// Calculate surface coverage
					int z1=0;
					int z2=0;
					int z3=0;
					int z4=0;
					int z5=0;
					int z6=0;
					int z7=0;
					int z8=0;
					int z9=0;
					int z10=0;
					int z0=0;
					for (int i=0; i<size; i++)
					{					
						for (int j=0; j<size; j++)
						{	
						
						if (s[i][j] == 0) z0=z0+1; else
							z0=z0+0;
							
							if (s[i][j] == 1) z1=z1+1; else
							z1=z1+0;
						
		
							if (s[i][j] == 2) z2=z2+1; else
							z2=z2+0;
		
							if (s[i][j] == 3) z3=z3+1; else
							z3=z3+0;
						
							if (s[i][j] == 4) z4=z4+1; else
							z4=z4+0;
					
							if (s[i][j] == 5) z5=z5+1; else
							z5=z5+0;
							
							if (s[i][j] == 6) z6=z6+1; else
							z6=z6+0;
							
							if (s[i][j] == 7) z7=z7+1; else
							z7=z7+0;
							if (s[i][j] == 8) z8=z8+1; else
							z8=z8+0;
							if (s[i][j] == 9) z9=z9+1; else
							z9=z9+0;
							
							if (s[i][j] == 10) z10=z10+1; else
							z10=z10+0;
						}
					}
					Z1=(double) z1/(L*L);
					Z2=(double) z2/(L*L);
					Z3=(double) z3/(L*L);
					Z4=(double) z4/(L*L);
					
					Z5=(double) z5/(L*L);
					Z6=(double) z6/(L*L);
					Z7=(double) z7/(L*L);

					Z8=(double) z8/(L*L);

					Z9=(double) z9/(L*L);
					
					Z10=(double) z10/(L*L);
					Z0=(double) z0/(L*L);
					
					
		
					
					if (N>10)
					{
					Zx0=(Zx0+Z0);
					Zx1=(Zx1+Z1);
					Zx2=(Zx2+Z2);
					Zx3=(Zx3+Z3);
					Zx4=(Zx4+Z4);
					Zx5=(Zx5+Z5);
					Zx6=(Zx6+Z6);
					Zx7=(Zx7+Z7);
					Zx8=(Zx8+Z8);
					Zx9=(Zx9+Z9);
					Zx10=(Zx10+Z10);
					}
					
					double zx0= Zx0/((N)-10);
					double zx1= Zx0/((N)-10);
					double zx2= Zx0/((N)-10);
					double zx3= Zx0/((N)-10);
					double zx4= Zx0/((N)-10);
					double zx5= Zx0/((N)-10);
					double zx6= Zx0/((N)-10);
					double zx7= Zx0/((N)-10);
					double zx8= Zx0/((N)-10);
					double zx9= Zx0/((N)-10);
					double zx10= Zx0/((N)-10);
					
					// Calculate rate
					double rH2 = RH2/1000;
					double rH2O = RH2O/1000;
					double rCO = RCO/1000;
					double rCO2 =  RCO2/1000;
					
					if (N>10)
					{
					RxH2 = (RxH2+RH2);
					RxH2O = (RxH2O+RH2O);
					RxCO = (RxCO+RCO);
					RxCO2 = (RxCO2+RCO2);
					}
					
					rxH2 =  RxH2/((N*1000)-10000);
					rxH2O =  RxH2O/((N*1000)-10000);
					rxCO =  RxCO/((N*1000)-10000);
					rxCO2 =  RxCO2/((N*1000)-10000);
					
					// Calculate selectivities
					double SH2 = rH2/(rH2+rH2O);
					double SCO = rCO/(rCO+rCO2);
					double SxH2 = rxH2/(rxH2+rxH2O);
					double SxCO = rxCO/(rxCO+rxCO2);
					
					
					// Show resalt in CM
					/*
					System.out.println("N    = " + N + "MCS");
					System.out.println("Z0  = " + Z0);
					System.out.println("Z1  = " + Z1);
					System.out.println("Z2  = " + Z2);
					System.out.println("Z3  = " + Z3);
					System.out.println("Z4  = " + Z4);
					System.out.println("Z5  = " + Z5);
					System.out.println("Z6  = " + Z6);
					System.out.println("Z7  = " + Z7);
					System.out.println("Z8  = " + Z8);
					System.out.println("Z9  = " + Z9);
					System.out.println("Z10  = " + Z10);
					System.out.println("rH2  = " + rH2);
					System.out.println("rH2O = " + rH2O);
					System.out.println("rCO  = " + rCO);
					System.out.println("rCO2 = " + rCO2);
					System.out.println("rxH2  = " + rxH2);
					System.out.println("rxH2O = " + rxH2O);
					System.out.println("rxCO  = " + rxCO);
					System.out.println("rxCO2 = " + rxCO2);					
					System.out.println("SH2 = " + SH2);					
					System.out.println("SCO = " + SCO);
					System.out.println("SxH2 = " + SxH2);					
					System.out.println("SxCO = " + SxCO);
					System.out.println("---------------------------------------------------------");
					*/
					System.out.println(N);
					System.out.println(Z0);
					System.out.println(Z1);
					System.out.println(Z2);
					System.out.println(Z3);
					System.out.println(Z4);
					System.out.println(Z5);
					System.out.println(Z6);
					System.out.println(Z7);
					System.out.println(Z8);
					System.out.println(Z9);
					System.out.println(Z10);
					System.out.println(rH2);
					System.out.println(rH2O);
					System.out.println(rCO);
					System.out.println(rCO2);
					System.out.println(rxH2);
					System.out.println(rxH2O);
					System.out.println(rxCO);
					System.out.println(rxCO2);					
					System.out.println(SH2);					
					System.out.println(SCO);
					System.out.println(SxH2);					
					System.out.println(SxCO);
					System.out.println(zx0);
					System.out.println(zx1);
					System.out.println(zx2);
					System.out.println(zx3);
					System.out.println(zx4);
					System.out.println(zx5);
					System.out.println(zx6);
					System.out.println(zx7);
					System.out.println(zx8);
					System.out.println(zx9);
					System.out.println(zx10);
					System.out.println(ktot);
					System.out.println("---------------------------------------------------------");					
				repaint();		// causes update method to be called soon
			}
			
			try { Thread.sleep(1); } catch (InterruptedException e) {}	// sleep time in milliseconds
		}
	}

    // Given a lattice site, compute energy change from hypothetical flip; note pbc:
    double deltaU(int i, int j) {
        int leftS, rightS, topS, bottomS;  // values of neighboring spins
        if (i == 0) leftS = s[size-1][j]; else leftS = s[i-1][j];
        if (i == size-1) rightS = s[0][j]; else rightS = s[i+1][j];
        if (j == 0) topS = s[i][size-1]; else topS = s[i][j-1];
        if (j == size-1) bottomS = s[i][0]; else bottomS = s[i][j+1];
        return 2.0 * s[i][j] * (leftS + rightS + topS + bottomS) -  2 * 0 * s[i][j];
    }

	// Color a given square according to the site's orientation:
	void colorSquare(int i, int j) {
	
	if (s[i][j] == 0) offScreenGraphics.setColor(emptyColor);
		else
		if (s[i][j] == 1) offScreenGraphics.setColor(oneColor);
            else
                if (s[i][j] == 2) offScreenGraphics.setColor(twoColor);
                  else
                    if (s[i][j] == 3) offScreenGraphics.setColor(threeColor);
						else
				            if (s[i][j] == 4) offScreenGraphics.setColor(fourColor);
								else
						            if (s[i][j] == 5) offScreenGraphics.setColor(fiveColor);
										else
								            if (s[i][j] == 6) offScreenGraphics.setColor(sixColor);
												else
										            if (s[i][j] == 7) offScreenGraphics.setColor(sevenColor);
														else
															if (s[i][j] == 8) offScreenGraphics.setColor(eightColor);
																else
																	if (s[i][j] == 9) offScreenGraphics.setColor(nineColor);
																		else
																			if (s[i][j] == 10) offScreenGraphics.setColor(tenColor);
																					
		offScreenGraphics.fillRect(i*squareWidth,j*squareWidth,squareWidth,squareWidth);
		
	}

	// Override default update method to skip drawing the background:
	public void update(Graphics g) {
		paint(g);
	}

	// Paint method just blasts the off-screen image to the screen:
	public void paint(Graphics g) {
		g.drawImage(offScreenImage,0,0,canvasSize,canvasSize,this);
	}

	// Main method just calls constructor to get started:
    public static void main(String[] args) {
		new CPO();
    }  
}