/*
 *  Java OTR library
 *  Copyright (C) 2008-2009  Ian Goldberg, Muhaimeen Ashraf, Andrew Chung,
 *                           Can Tang
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of version 2.1 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package ca.uwaterloo.crysp.otr.crypt.rim;

import ca.uwaterloo.crysp.otr.test.TestHarness;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.*;

class RIMRunTest extends UiApplication {
    private RIMRunTest() {
        pushScreen(new TestScreen());
    }
    /*
     * public static void main(String[] args) { RIMRunTest theApp = new
     * RIMRunTest(); theApp.enterEventDispatcher(); }
     */
}

final class TestScreen extends MainScreen {
    TestScreen() {
        LabelField title = new LabelField("OTR Test", LabelField.ELLIPSIS
                | LabelField.USE_ALL_WIDTH);
        setTitle(title);
        RIMProvider prov = new RIMProvider();
        add(new RichTextField(TestHarness.execute(prov), Field.NON_FOCUSABLE));
    }
}
