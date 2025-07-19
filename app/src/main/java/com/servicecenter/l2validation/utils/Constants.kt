package com.servicecenter.l2validation.utils

import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.data.local.entities.DashboardEnum
import com.servicecenter.l2validation.data.local.entities.DashboardItem

// Code reviewed

object Constants {
    val ITEM_TYPE= "ITEM_TYPE"

    const val STATUS_CODE: Int = 200
    const val OTP_TIMER: Long = 30000
    const val OTP_INTERVAL: Long = 1000
    const val awb: String = ""
    const val CHECK_IMAGE = "CHECK_IMAGE"
    const val CHECK = "CHECK"
    const val TITLE = "TITLE"
    const val MESSAGE = "MESSAGE"
    const val SCREEN_TYPE = "SCREEN_TYPE"
    const val CHECK_INPUT = "CHECK_INPUT"
    const val FLYER = "FLYER"
    const val SHIPMENT_LABEL = "SHIPMENT_LABEL"
    const val _NEWLAND = "NEWLAND:MT65"
    const val _NEW_NEWLAND = "DROI:NLS-MT90"
    const val _NEWLAND_T90 = "NEWLAND:NLS-MT90"
    const val FLYER_CODE = "FLYER_CODE"

    //const val REGEX = "^[0-9]{9,12}$"
    const val REGEX = "^[0-9]{9,12}$[.*\\n]*"
    const val AUTH_TOKEN = "AUTH_TOKEN"
    const val app_code = "app_code"
    const val auth_required = "auth_required"
    const val DEVICE = "Device"
    const val app_version = "app_version"
    const val CAMERA = android.Manifest.permission.CAMERA
    const val LOCATION_FINE = android.Manifest.permission.ACCESS_FINE_LOCATION
    const val LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION
    const val scanned_AWB = "scanned_AWB"
    const val shiment_label_status = "shiment_label_status"
    const val flyerRelatedAirwillNo = "flyerRelatedAirwillNo"
    const val failure = "Failure"
    const val FRONT_IMAGE = "FRONT"
    const val BACK_IMAGE = "BACK"
    const val FRONT_IMAGE_ID = "FRONT_ID"
    const val BACK_IMAGE_ID = "BACK_ID"
    const val l1_qc_validation_required="l1_qc_validation_required"

    const val FRONT_RTS_KEY = "FRONT_KEY"
    const val BACK_RTS_KEY = "BACK_KEY"
    const val SHIPMENT_LABEL_IMAGE_ID = "SHIPMENT_LABEL_IMAGE_ID"
    const val qc_parameter_id = "qc_parameter_id"
    const val answer = "answer"

    // DB constant for status
    const val pending = "Pending"
    const val Failed = "Failed"
    const val Success = "Success"

    const val FE_IMAGE_MATCHED = "FE_IMAGE_MATCHED"

    //Yes or No Status
    const val Yes = "Yes"
    const val No = "No"

    const val ANSWERS_LIST = "ANSWERS_LIST"
    const val STATUS = "STATUS"
    const val FALIURE = "failure"

    //UD Calling
    const val Is_UdCalling="is_udCalling"
    const val AWB_NUMBER="AWB_NUMBER"
    const val UD_STATUS="UD_STATUS"
    const val DRS_ID="DRS_ID"
    const val ORDER_ID="ORDER_ID"
    const val ACTIONALBLE_DAYS="ACTIONALBLE_DAYS"
    const val SELECTED_DATE="SELECTED_DATE"
    const val EVENT="EVENT"
    const val FE_NUMBER="FE_NUMBER"
    const val UD_TYPE="UD_TYPE"
    const val REQUEST_TYPE="REQUEST_TYPE"
    const val PAYMENT_TYPE="PAYMENT_TYPE"
    const val CLIENT_CORRELATION_ID="CLIENT_CORRELATION_ID"
    const val RESCHEDULE_REMARKS="RESCHEDULE_REMARKS"
    const val PENDING="Pending"
    const val VALIDATED="Validated"
    const val NORESPONSE="NoResponse"


    const val FE_LAT="FE_LAT"
    const val FE_LONG="FE_LONG"
    const val CONSIGNEE_LAT="CONSIGNEE_LAT"
    const val CONSIGNEE_LONG="CONSIGNEE_LONG"
    const val REMARK="REMARK"



    //rtsKeys
    const val Is_RTS = "is_rts"

    const val IMAGE_MATCH = "IMAGE_MATCH"
    const val LINK_FLYER_ACTIVITY = "LINK_FLYER_ACTIVITY"
    const val SUCCESS_LINK_FLYER_ACTIVITY = "SUCCESS_LINK_FLYER_ACTIVITY"
    const val SHIPMENT_LABEL_TYPE = "SHIPMENT_LABEL_TYPE"
    const val FLYER_AWB = "FLYER_AWB"
    const val FE_IMAGE_CLEAR = "FE_IMAGE_CLEAR"
    const val FLYER_PROPERLY_SEALED = "FLYER_PROPERLY_SEALED"


    const val SCREEN_KEY = "Screen"
    const val BUTTON_KEY = "Button"
    const val SCAN_KEY = "Scan"
    const val CHECKBOX_KEY = "CheckBox"

    const val LOGIN_EVENT = "Login"
    const val FORGOT_PASSWORD_EVENT = "ForgotPassword"
    const val APP_DASHBOARD_EVENT = "APP_DASHBOARD_EVENT"
    const val PROFILE_EVENT = "Profile"
    const val CHANGE_PASSWORD_EVENT = "ChangePassword"
    const val FLYER_BARCODE_SCAN = "Flyer_Barcode_Scan"
    const val PENDING_LIST = "Pending List"
    const val FAILED_LIST = "Failed List"
    const val CAPTURE_SHIPMENT_LABEL_IMAGE = "Capture_Shipment_Label_Image"
    const val SHIPMENT_LABEL_SCAN = "Shipment_Label_Scan"
    const val CAPTURE_SHIPMENT_FRONT_IMAGE = "Capture_Shipment_Front_Image"
    const val CAPTURE_SHIPMENT_BACK_IMAGE = "Capture_Shipment_Back_Image"
    const val L2_BARCODE_LABEL_MATCH = "L2_Barcode_Label_Match"
    const val L2_BARCODE_LABEL_MISMATCH = "L2_Barcode_Label_Mismatch"
    const val FE_IMAGE_VALIDATION = "FE_Image_Validation"
    const val L2_BARCODE_FINAL_SUCCESS = "L2_Validation_Final_Success"
    const val ANSWER_LIST = "ANSWER_LIST"
    const val QUESTION_FRAGMENT = "Question_Fragment"
    const val FE_IMAGES = "fe_images"
    const val PRODUCT_IMAGES = "product_images"
    const val FLYER_IMAGES = "flyer_images"
    const val RTS_LIST_EVENT_1 = "Rts Pending List 1"
    const val RTS_LIST_EVENT_2 = "Rts Pending List 2"
    const val FILTER_RTS_EVENT = "Filter Rts Event"


    const val FORGOT_PASSWORD = "Forget_Password"
    const val GET_CODE = "Get_Code"
    const val VERIFY_CODE = "Verify_Code"
    const val RESEND_OTP_LOGIN = "Resend_OTP_Login"
    const val SEND_CODE = "Send_Code"
    const val RESEND_OTP_FORGOT_PASSWORD = "Resend_OTP_Forgot_Password"
    const val TILES = "Tiles"
    const val PROFILE = "Profile"
    const val CHANGE_PASSWORD = "Change Password"
    const val LOGOUT = "Logout"
    const val RESEND_CODE_CHANGE_PASSWORD = "Resend_Code_Change_Password"
    const val RESET_PASSWORD = "Reset_Password"
    const val PENDING_SHIPMENTS = "Pending_Shipments"
    const val FAILED_SHIPMENTS = "Failed_Shipments"
    const val CAPTURE_SHIPMENT_LABEL_IMAGE_UPLOAD = "Capture_Shipment_Label_Image_Upload"
    const val CAPTURE_SHIPMENT_FRONT_IMAGE_UPLOAD = "Capture_Shipment_Front_Image_Upload"
    const val CAPTURE_SHIPMENT_BACK_IMAGE_UPLOAD = "Capture_Shipment_Back_Image_Upload"
    const val CAPTURE_SHIPMENT_LABEL_IMAGE_RETAKE = "Capture_Shipment_Label_Image_Retake"
    const val CAPTURE_SHIPMENT_FRONT_IMAGE_RETAKE = "Capture_Shipment_Front_Image_Retake"
    const val CAPTURE_SHIPMENT_BACK_IMAGE_RETAKE = "Capture_Shipment_Back_Image_Retake"
    const val SCAN_NEXT_SHIPMENT_LABEL = "Scan_Next_Shipment_Label"
    const val RVP_LABEL_NOT_READABLE = "RVP_Label_Not_Readable"
    const val SCAN_NEXT_SHIPMENT_MISMATCH = "Scan_Next_Shipment_Mismatch"
    const val FE_IMAGE_VALIDATION_YES = "FE_Image_Validation_Yes"
    const val FE_IMAGE_VALIDATION_NO = "FE_Image_Validation_No"
    const val FE_IMAGE_VALIDATION_SUBMIT = "FE_Image_Validation_Submit"
    const val SCAN_NEXT_SHIPMENT_L2_SUCCESS = "Scan_Next_Shipment_L2_Success"


    const val RVP_SHIPMENT_VALIDATION_VALUE = "RVP_Shipment_Validation"
    const val EMPLOYEE_PROFILE_VALUE = "Employee_Profile"
    const val CHANGE_PASSWORD_VALUE = "Change_Password"
    const val LOGOUT_YES_VALUE = "Logout_Yes"
    const val FILTER = "filter"
    const val FILTER_DATA = "filter_date"
    const val MANUAL_SEARCH = "MANUAL_SEARCH"
    const val FILTER_UD_DATA = "FILTER_UD_DATA"

    const val KEY_UPDATE_REQUIRED = "android_force_update_required"
    const val DASHBOARD_EVENT_MANAGEMENT = "dashboard_event_management"
    const val KEY_REQUIRED_VERSION = "android_force_update_required_version"
    const val KEY_UPDATE_URL = "android_force_update_store_url"
    const val KEY_DESCRIPTION = "android_force_update_description"
    const val MINIMUM_FETCH_INTERVAL = 60L

    const val ESPER_TOKEN: String = "UgOF8lq0PgkHHrGPy2oM7N3SObPAUZ"
    const val IMAGE_QUALITY_THRESHOLD = 150

    const val REQUEST_CHECK_SETTINGS = 200
    const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    const val request_flag="request_flag"

    const val CALL_DROP="CALL_DROP"
    const val CALL_ACTIVE="CALL_ACTIVE"
    const val PERMISSION_REQUEST_CODE = 1001
    const val release = "release"

    val dashboardItemList = mutableListOf(
        DashboardItem(R.drawable.rvp_shipment_logo, "RVP Shipment\nValidation",DashboardEnum.RVP_VALIDATION),
      //  DashboardItem(R.drawable.handover, "Handover",DashboardEnum.RVP_HANDOVER),
    //    DashboardItem(R.drawable.ud_calling, "UD Calling",DashboardEnum.UD_CALLING),
        DashboardItem(R.drawable.rts_scan, "RTS Creation",DashboardEnum.RTS),
        DashboardItem(R.drawable.ic_sal_tally, "SAL Tally",DashboardEnum.SAL_TALLY)
    )
    object BundleConstants{
        val HEADER_NAME = "headerName"
        val AWB_NO = "awbNo"
        const val BUNDLE="bundle"
        const val IMAGE_TYPE="imageType"
        const val IMAGE_PATH="imagePath"
        val IS_ORDER_TYPE = "isOrderType"
        val ORDER_TYPE = "orderType"
        val MSG_BUNDLE = "messageBundle"
        val IS_SUB = "isSub"
        val SUB_MSG = "subMsg"
        val MSG = "msg"
        val STATUS = "status"
        val BTN_TEXT = "btnText"
        val BTN_ACTION = "btnAction"
    }

    const val TALLY_NOT_STARTED = 0//Dashboard Activity
    const val TALLY_STARTED = 1 // ScanFragment
    const val TALLY_MARKED_COMPLETED = 2//MarkComplete Fragment
    const val TALLY_COMPLETED = 3 //Dashboard Activity and user can't start tally again
}