
#import "RNFingerprintChange.h"
#import <LocalAuthentication/LocalAuthentication.h>

@implementation RNFingerprintChange

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()


RCT_EXPORT_METHOD(hasFingerPrintChanged:(RCTResponseSenderBlock)errorCallback successCallback:(RCTResponseSenderBlock)successCallback)
{
    BOOL changed = NO;

    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;

    
    if ([context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error]) {
        
        [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthentication error:nil];
        NSData *domainState = [context evaluatedPolicyDomainState];
        
        // load the last domain state from touch id
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        NSData *oldDomainState = [defaults objectForKey:@"domainTouchID"];
        
        if (oldDomainState)
        {
            // check for domain state changes
            
            if ([oldDomainState isEqual:domainState])
            {
                NSLog(@"nothing changed.");
            }
            else
            {
                changed = YES;
                NSLog(@"domain state was changed!");
            }
        }

        // save the domain state that will be loaded next time
        [defaults setObject:domainState forKey:@"domainTouchID"];
        [defaults synchronize];
        
        successCallback(@[[NSNumber numberWithBool:changed]]);
    }
}


@end
  
