import React from 'react';
import PropTypes from 'prop-types';
import {MODULE_NAME} from '../constants';
import styles from './GlobalLinkLogo.module.css';

const contextPath = () => globalThis.contextJsParameters?.contextPath || '';

// The GlobalLink wordmark. Decorative: the title beside it already names GlobalLink.
export const GlobalLinkLogo = () => (
    <img className={styles.logo}
         src={`${contextPath()}/modules/${MODULE_NAME}/img/globalLink.png`}
         alt=""
         width="68"
         height="28"/>
);

// Header title with the logo first, top left, centred on the title text. Moonstone's Header renders its
// title inside its h1, so the heading's name stays the text alone.
export const LogoTitle = ({children}) => (
    <span className={styles.title}>
        <GlobalLinkLogo/>
        <span className={styles.text}>{children}</span>
    </span>
);

LogoTitle.propTypes = {
    children: PropTypes.node.isRequired
};
