// My AIDL file, named SomeClass.aidl
// Note that standard comment syntax is respected.
// Comments before the import or package statements are not bubbled up
// to the generated interface, but comments above interface/method/field
// declarations are added to the generated interface.

// Include your fully-qualified package statement.
package urbanstew.RehearsalAssistant;


// Declare the interface.
interface IRecordService
{
	void toggleRecording(long SessionId);
	void startRecording(long SessionId);
	void stopRecording();
	void startSession(long SessionId);
	void stopSession(long SessionId);
	int getState();
	long getTimeInRecording();
	long getTimeInSession();
	int getMaxAmplitude();
	void setSession(long sessionId);
}