INSERT INTO public.payment_method ("name",description,is_default,is_online,instruction_url,created_at,updated_at,logo_url,is_mobile_money,code) values
	 ('TeleBirr','Mobile wallet offline deposit. Please follow instructions to deposit manually.',false,false,'/deposit/offline/instructions/telebirr','2025-10-25 23:32:27.031862+01','2025-10-25 23:32:27.031862+01','/logo/telebirr.png',true,'telebirr'),
	 ('CBE Bank','CBE Bank',false,false,'/deposit/offline/instructions/cbebank','2025-10-27 23:35:35.539909+00','2025-10-27 23:35:35.539909+00','/logo/cbe.png',false,'cbebank'),
	 ('MPesa','MPesa payment',false,false,'/deposit/offline/instructions/mpesa','2025-10-26 16:48:49.661561+00','2025-10-26 16:48:49.661561+00','/logo/mpesa.png',true,'mpesa'),
	 ('CBE Birr','CBE Birr',false,false,'/deposit/offline/instructions/cbebirr','2025-10-26 16:49:43.378943+00','2025-10-26 16:49:43.378943+00','/logo/cbebirr.png',true,'cbe'),
	 ('AddisPay','Online payment gateway for instant payments.',true,true,'/deposit/checkout/addispay','2025-10-25 23:32:27.031862+01','2025-10-25 23:32:27.031862+01',NULL,true,'addispay');
