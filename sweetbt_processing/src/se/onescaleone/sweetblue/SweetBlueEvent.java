package se.onescaleone.sweetblue;

/*
 *  Written by Andreas Göransson & David Cuartielles, 1scale1 Handelsbolag, 
 *  for use in the project SweetBlue.
 *  
 *  SweetBlue: a library and communication protocol used to set Arduino 
 *  states over bluetooth from an Android device. Effectively removing 
 *  the need to program the Arduino chip.
 *  
 *  Copyright (C) 2011  1scale1 Handelsbolag
 *
 *  This file is part of SweetBlue.
 *
 *  SweetBlue is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SweetBlue is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SweetBlue.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.EventObject;

public class SweetBlueEvent extends EventObject {

	private boolean connected;

	public SweetBlueEvent(SweetBlue src, boolean connected) {
		super(src);
		this.connected = connected;
	}

	public boolean getConnected() {
		return connected;
	}
}
