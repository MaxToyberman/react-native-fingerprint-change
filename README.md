
# react-native-fingerprint-change

## Getting started

`$ npm install react-native-fingerprint-change --save`

Note: This module works only for iOS for now
### Mostly automatic installation

`$ react-native link react-native-fingerprint-change`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-fingerprint-change` and add `RNFingerprintChange.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNFingerprintChange.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<


## Usage
```javascript
import RNFingerprintChange from 'react-native-fingerprint-change';

// TODO: What to do with the module?

RNFingerprintChange.hasFingerPrintChanged((error, fingerprintHasChanged)=>{
	if(fingerprintHasChanged) {
		// do what you need when fingerprint change has been detected
	}
})
		
```
  