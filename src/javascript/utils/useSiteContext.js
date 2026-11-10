import {shallowEqual, useSelector} from 'react-redux';

// The site and languages open in jContent. Never contextJsParameters.siteKey: that is the site the server
// resolved for the request, systemsite wherever sites share the server name localhost.
export const useSiteContext = () => useSelector(state => ({
    siteKey: state.site,
    language: state.language,
    uilang: state.uilang || 'en'
}), shallowEqual);
