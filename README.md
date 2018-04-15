
# react-native-fingerprint-change

## Getting started

`$ npm install react-native-fingerprint-change --save`

### Mostly automatic installation

`$ react-native link react-native-fingerprint-change`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-fingerprint-change` and add `RNFingerprintChange.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNFingerprintChange.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.toyberman.fingerprintChange.RNFingerprintChangePackage;` to the imports at the top of the file
  - Add `new RNReactNativeAccessibilityPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-fingerprint-change'
  	project(':react-native-fingerprint-change').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-fingerprint-change/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-fingerprint-change')
  	```


## Usage
```javascript
import RNFingerprintChange from 'react-native-fingerprint-change';

// How to use the module 
RNFingerprintChange.hasFingerPrintChanged((error, fingerprintHasChanged)=>{
	if(fingerprintHasChanged) {
		// do what you need when fingerprint change has been detected
	}
})


example:

import {
    AppState,
} from 'react-native';

import RNFingerprintChange from 'react-native-fingerprint-change';


class example extends Component {

	constructor(props) {
		super(props)
		this.state = {
			appState: AppState.currentState,
		}
	}

	componentDidMount() {
        AppState.addEventListener('change', this._handleAppStateChange);
	}

	componentWillUnmount() {
    	AppState.removeEventListener('change', this._handleAppStateChange);
    }

	_handleAppStateChange = (nextAppState) => {
		if (this.state.appState.match(/inactive|background/) && nextAppState === 'active') {
			//App has come to the foreground!
			RNFingerprintChange.hasFingerPrintChanged((error) => {},(fingerprintHasChanged)=>{
			if(fingerprintHasChanged) {
				// do what you need 
			}
		})
		}
		this.setState({appState: nextAppState});
	}
}
```
  
